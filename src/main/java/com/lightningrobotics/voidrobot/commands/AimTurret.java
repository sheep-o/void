package com.lightningrobotics.voidrobot.commands;

import java.util.function.DoubleSupplier;

import com.lightningrobotics.voidrobot.subsystems.Turret;
import com.lightningrobotics.voidrobot.subsystems.Vision;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class AimTurret extends CommandBase {
    
    // Creates our turret and vision subsystems
    private Turret turret;
    private Vision vision;

    private static double targetAngle = 0; // this is the angle that we are setting to the turret


    private static double offsetAngle = 0d; // The offset that vision gives us
    private static double currentAngle; // The current angle of the turret
    private DoubleSupplier controllerInput;

    public AimTurret(Turret turret, Vision vision) {
        this.turret = turret;
        this.vision = vision;

        // Not adding vision since its use is read-only
        addRequirements(turret, vision);
    }

    @Override
    public void initialize() {}

    @Override
    public void execute() {
        // offsetAngle += controllerInput.getAsDouble();
        offsetAngle  = vision.getOffsetAngle(); // gets the vision offset angle 
        turret.setVisionOffset(offsetAngle); // setting the target angle of the turret
    }

    @Override
    public void end(boolean interrupted) {
        turret.stopTurret();
    }

    @Override
    public boolean isFinished() {
        return false;
    }

}
