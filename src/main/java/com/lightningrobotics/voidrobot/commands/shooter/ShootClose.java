package com.lightningrobotics.voidrobot.commands.shooter;

import com.lightningrobotics.voidrobot.constants.Constants;
import com.lightningrobotics.voidrobot.subsystems.Indexer;
import com.lightningrobotics.voidrobot.subsystems.Shooter;
import com.lightningrobotics.voidrobot.subsystems.Turret;
import com.lightningrobotics.voidrobot.subsystems.Vision;

import edu.wpi.first.wpilibj2.command.CommandBase;

public class ShootClose extends CommandBase {

	private Shooter shooter;
	private Indexer indexer;
	private Turret turret;
	private Vision vision;

	public ShootClose(Shooter shooter, Indexer indexer, Turret turret, Vision vision) {

		this.shooter = shooter;
		this.indexer = indexer;
		this.turret = turret;
		this.vision = vision;

		addRequirements(shooter, indexer); // not adding vision or turret as it is read onl
	}
	@Override
	public void initialize() {
		turret.setManualOverride(true);
		turret.setTarget(0d);
		vision.turnOffVisionLight();
	}

	@Override
	public void execute() {
		shooter.setRPM(Constants.SHOOT_LOW_RPM);
		shooter.setHoodAngle(Constants.SHOOT_LOW_ANGLE);
		
		if(shooter.getArmed() && turret.getArmed()) {
			indexer.toShooter();
		}
	}

	@Override
	public void end(boolean interrupted) {
		shooter.stop();
		indexer.stop();
		turret.setManualOverride(false);
	}

	@Override
	public boolean isFinished() {
		return false;
	}
	
}
