// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.lightningrobotics.voidrobot.commands.turret;

import java.util.function.DoubleSupplier;

import javax.security.sasl.RealmCallback;

import com.lightningrobotics.common.controller.PIDFController;
import com.lightningrobotics.common.subsystem.core.LightningIMU;
import com.lightningrobotics.common.util.LightningMath;
import com.lightningrobotics.voidrobot.constants.Constants;
import com.lightningrobotics.voidrobot.subsystems.Drivetrain;
import com.lightningrobotics.voidrobot.subsystems.Turret;
import com.lightningrobotics.voidrobot.subsystems.Vision;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class AimTurret extends CommandBase {

    private final Vision vision;
    private final Turret turret;

    private LightningIMU imu;

    private double targetAngle;
    private double constrainedAngle;
    private double initialIMUHeading; 

    private static ShuffleboardTab turretTab = Shuffleboard.getTab("Turret");
    private static NetworkTableEntry displayOffset  = turretTab.add("vision offset", 0).getEntry();
    private static NetworkTableEntry displayTargetAngle = turretTab.add("target angle", 0).getEntry();
    private static NetworkTableEntry displayConstrainedAngle = turretTab.add("constrained angle", 0).getEntry();
    private static NetworkTableEntry displayMotorOutput = turretTab.add("motor output", 0).getEntry();

    private static double motorOutput;
    private DoubleSupplier controllerInputX;
    private DoubleSupplier controllerInputY;
    private final Drivetrain drivetrain;

    private double targetOffset;
    private double lastKnownHeading = 0;
    private double lastKnownDistance = 6.4008;
    private boolean isUsingOdometer = true;
    private double initialOdometerGyroReading = 0d;
    private double initialX = 0d;
    private double initialY = 0d;

    enum TargetingState{
        MANUAL,
        VISION,
        NO_VISION
    }
    TargetingState targetingState;

    public AimTurret(Vision vision, Turret turret, Drivetrain drivetrain, LightningIMU imu, DoubleSupplier controllerInputX, DoubleSupplier controllerInputY) {
        this.vision = vision;
        this.drivetrain = drivetrain;
        this.turret = turret;
        this.imu = imu;
        this.controllerInputX = controllerInputX;
        this.controllerInputY = controllerInputY;

        addRequirements(vision, turret);
    }

    @Override
    public void initialize() {

        targetingState = TargetingState.MANUAL;

        drivetrain.resetPose();
        lastKnownHeading = turret.getCurrentAngle().getDegrees();
        initialIMUHeading = imu.getHeading().getDegrees();

    }

    @Override
    public void execute() {

        if (controllerInputX.getAsDouble() == 0) { // vision.getDistance == -1
            targetingState = TargetingState.NO_VISION;
        } else {
            targetingState = TargetingState.MANUAL;
        }
            
        switch(targetingState) {
            case MANUAL: 
                motorOutput = controllerInputX.getAsDouble() / 4;
                break;
            case VISION:
                isUsingOdometer = true;
                targetOffset = vision.getOffsetAngle();
                lastKnownDistance = vision.getTargetDistance();
                targetAngle = turret.getCurrentAngle().getDegrees() + targetOffset;

                targetAngle = turret.getConstrainedAngle(targetAngle);
                motorOutput = turret.getMotorOutput(targetAngle);
                break;
            case NO_VISION:
                if(isUsingOdometer){
                    isUsingOdometer = false;
                    //drivetrain.resetPose();
                    initialOdometerGyroReading = drivetrain.getPose().getRotation().getDegrees();
                    initialX = drivetrain.getPose().getX();
                    initialY = drivetrain.getPose().getY();
                    lastKnownHeading = turret.getCurrentAngle().getDegrees();
                }

                double relativeX = drivetrain.getPose().getX() - initialX;
                double relativeY = drivetrain.getPose().getY() - initialY;

                // rotate from odometer-center to robot-center
                relativeX = turret.rotateX(relativeX, relativeY, initialOdometerGyroReading);
                relativeY = turret.rotateY(relativeX, relativeY, initialOdometerGyroReading);

                // update rotation data 
                double changeInRotation = drivetrain.getPose().getRotation().getDegrees() - initialOdometerGyroReading;
                SmartDashboard.putNumber("odometer x", relativeX);
                SmartDashboard.putNumber("odometer y", relativeY);
                SmartDashboard.putNumber("change in rotation", changeInRotation);

                targetAngle = turret.getTargetNoVision(relativeX, relativeY, lastKnownHeading, lastKnownDistance, changeInRotation);
                
                targetAngle = turret.getConstrainedAngle(targetAngle);
                motorOutput = turret.getMotorOutput(targetAngle);
                break;   
        }

        displayOffset.setDouble(targetOffset); // offsetAngle.getDegrees()
        displayTargetAngle.setDouble(targetAngle);

        /*
        double sign = Math.signum(targetAngle);
        targetAngle =  sign * (((Math.abs(targetAngle) + 180) % 360) - 180);

        constrainedAngle = LightningMath.constrain(targetAngle, Constants.MIN_TURRET_ANGLE, Constants.MAX_TURRET_ANGLE);
        //displayConstrainedAngle.setDouble(constrainedAngle);

        if(constrainedAngle - turret.getCurrentAngle().getDegrees() <= Constants.SLOW_PID_THRESHOLD) {
            motorOutput = Constants.TURRET_PID_SLOW.calculate(turret.getCurrentAngle().getDegrees(), constrainedAngle);
        } else {
            motorOutput = Constants.TURRET_PID_FAST.calculate(turret.getCurrentAngle().getDegrees(), constrainedAngle);
        }
        */

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
}
