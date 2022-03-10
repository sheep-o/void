// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.lightningrobotics.voidrobot.commands.turret;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import com.fasterxml.jackson.databind.ser.std.BooleanSerializer;
import com.lightningrobotics.common.subsystem.core.LightningIMU;
import com.lightningrobotics.common.util.LightningMath;
import com.lightningrobotics.common.util.filter.MovingAverageFilter;
import com.lightningrobotics.voidrobot.constants.Constants;
import com.lightningrobotics.voidrobot.subsystems.Drivetrain;
import com.lightningrobotics.voidrobot.subsystems.Turret;
import com.lightningrobotics.voidrobot.subsystems.Vision;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class AimTurret extends CommandBase {

    private final Vision vision;
    private final Turret turret;

    private LightningIMU imu;

    private double targetAngle;
    private double constrainedAngle;
    private double initialIMUHeading; 

    private static ShuffleboardTab turretTab = Shuffleboard.getTab("Turret");
    private static ShuffleboardTab trimTab = Shuffleboard.getTab("Biases");
    private static NetworkTableEntry displayOffset  = turretTab.add("vision offset", 0).getEntry();
    private static NetworkTableEntry displayTargetAngle = turretTab.add("target angle", 0).getEntry();
    private static NetworkTableEntry displayConstrainedAngle = turretTab.add("constrained angle", 0).getEntry();
    private static NetworkTableEntry displayMotorOutput = turretTab.add("motor output", 0).getEntry();
    private static NetworkTableEntry manualOverrideEntry = trimTab.add("Manual Turret", false).getEntry();
	private static NetworkTableEntry turretTrimEntry = trimTab.add("Turret Bias", 0).getEntry();

    private static double motorOutput;
    private DoubleSupplier controllerInputX;
    private DoubleSupplier POV;
    private final Drivetrain drivetrain;

    private double targetOffset;
    private double turretTrim = 0d; 
    private double lastKnownHeading = 0;
    private double lastKnownDistance = 2.7432;
    private boolean isUsingOdometer = true;
    private double initialOdometerGyroReading = 0d;
    private double initialX = 0d;
    private double initialY = 0d;

    private BooleanSupplier syncVision;

	private MovingAverageFilter maf = new MovingAverageFilter(3);

    enum TargetingState{
        MANUAL,
        VISION,
        NO_VISION,
        MANUAL_OVERRIDE
    }
    
	private TargetingState targetingState;

    public AimTurret(Vision vision, Turret turret, Drivetrain drivetrain, LightningIMU imu, DoubleSupplier controllerInputX, DoubleSupplier POV, BooleanSupplier syncVision) {
        this.vision = vision;
        this.drivetrain = drivetrain;
        this.turret = turret;
        this.imu = imu;
        this.controllerInputX = controllerInputX;
        this.POV = POV;
        this.syncVision = syncVision;

        addRequirements(vision, turret);

		// displayOffset = turretTab.add("test offset", 0).getEntry();
        // displayTargetAngle = turretTab.add("target angle", 0).getEntry();
        // displayConstrainedAngle = turretTab.add("constrained angle", 0).getEntry();
        // displayMotorOutput = turretTab.add("motor output", 0).getEntry();

    }

    @Override
    public void initialize() {

        targetingState = TargetingState.NO_VISION;

        lastKnownHeading = turret.getCurrentAngle().getDegrees();
        initialIMUHeading = imu.getHeading().getDegrees();

    }

    @Override
    public void execute() {
        turretTrim = turretTrimEntry.getDouble(0);

        if (turret.getManualOverride()){
            targetingState = TargetingState.MANUAL_OVERRIDE;
        } else {
            if (manualOverrideEntry.getBoolean(false)){
                targetingState = TargetingState.MANUAL;
            } else if (vision.hasVision()) {
                targetingState = TargetingState.VISION;
            }
            else{
                targetingState = TargetingState.NO_VISION;
            }
        }
   
		System.out.println("TURRET STATE --------------------- " + targetingState + "--------------------------------------------");
        switch(targetingState) {
            case MANUAL: 
                motorOutput = POV.getAsDouble() * Constants.TURRET_MANUAL_SPEED_MULTIPLIER;
				isUsingOdometer = true;
                break;
            case VISION:
                isUsingOdometer = true;
                targetOffset = vision.getOffsetAngle();
                lastKnownDistance = vision.getTargetDistance();
                targetAngle = turret.getCurrentAngle().getDegrees() + targetOffset;
                targetAngle += turretTrim;

				targetAngle = maf.filter(targetAngle);

                turret.setTarget(targetAngle);
                motorOutput = turret.getMotorOutput(turret.getTarget());
                break;
            case NO_VISION:
                if(isUsingOdometer){
                    isUsingOdometer = false;
                    resetPose();
                    turretTrim = 0;

                }

                // if (syncVision.getAsBoolean() && vision.hasVision()/* || vision.hasVision()*/){
                //     targetOffset = vision.getOffsetAngle();
                //     lastKnownDistance = Units.feetToMeters(vision.getTargetDistance());
                //     vision.startTimer();
                //     isUsingOdometer = true;
                //     vision.setGoodDistance();
                // }

                // lastKnownDistance = vision.getTargetDistance();

                //turretTrim += POVToStandard(POV); <-- TODO: Test this

                double relativeX = drivetrain.getPose().getX() - initialX;
                double relativeY = drivetrain.getPose().getY() - initialY;

                // rotate from odometer-center to robot-center
                relativeX = turret.rotateX(relativeX, relativeY, initialOdometerGyroReading);
                relativeY = turret.rotateY(relativeX, relativeY, initialOdometerGyroReading);

                // update rotation data 
                double changeInRotation = drivetrain.getPose().getRotation().getDegrees() - initialOdometerGyroReading;

                targetAngle = turret.getTargetNoVision(relativeX, relativeY, lastKnownHeading, lastKnownDistance, changeInRotation) + targetOffset;
                // targetOffset = 0;

                targetAngle += turretTrim;
                turret.setTarget(targetAngle);
                motorOutput = turret.getMotorOutput(turret.getTarget());
                break;   
            case MANUAL_OVERRIDE:
                targetAngle = turret.getTarget();
                targetAngle += turretTrim;
                turret.setTarget(targetAngle);
                motorOutput = turret.getMotorOutput(turret.getTarget());
                break;
        }

        displayOffset.setDouble(targetOffset); // offsetAngle.getDegrees()
        displayTargetAngle.setDouble(targetAngle);
        displayMotorOutput.setDouble(motorOutput);
        
        turret.setPower(motorOutput);

    }

    @Override
    public void end(boolean interrupted) {
        turret.stop();
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    public void resetPose(){
        initialOdometerGyroReading = drivetrain.getPose().getRotation().getDegrees();
        initialX = drivetrain.getPose().getX();
        initialY = drivetrain.getPose().getY();
        lastKnownHeading = turret.getCurrentAngle().getDegrees();
    }

    public double POVToStandard(DoubleSupplier POV){
        if (POV.getAsDouble() == 90){
            return -1;
        } else if (POV.getAsDouble() == 270){
            return 1;
        } else {
            return 0;
        }
    }

}
