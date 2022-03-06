package com.lightningrobotics.voidrobot.commands.shooter;

import com.lightningrobotics.voidrobot.subsystems.Shooter;

import edu.wpi.first.wpilibj2.command.CommandBase;

public class RunShooterDashboard extends CommandBase {
    
    // Creates the shooter subsystem
    private Shooter shooter;

    public RunShooterDashboard(Shooter shooter) {
        this.shooter = shooter;
        addRequirements(shooter);
    }

    @Override
    public void initialize() {}

    @Override
    public void execute() {
        shooter.setRPM(shooter.getRPMFromDashboard());  // shooter.getRPMFromDashboard() // Gets the desired RPM from the dashboard and sets them to the motor
        shooter.setHoodAngle(shooter.getHoodAngleFromDashboard());
    }

    @Override
    public void end(boolean interrupted) {

    }

    @Override
    public boolean isFinished() {
        return false;
    }
	
}
