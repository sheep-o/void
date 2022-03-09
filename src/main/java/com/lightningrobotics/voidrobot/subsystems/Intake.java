package com.lightningrobotics.voidrobot.subsystems;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.TalonSRXControlMode;
import com.ctre.phoenix.motorcontrol.VictorSPXControlMode;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;
import com.lightningrobotics.voidrobot.constants.RobotMap;

import edu.wpi.first.wpilibj.motorcontrol.Talon;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Intake extends SubsystemBase {

	// Creates our intake motor
	private final VictorSPX intakeMotor;
	private final TalonSRX winch;
	private boolean isDeployed = false;
	private boolean isRetracted = false;


	public Intake() {
		// Sets the ID of the intake motor
		intakeMotor = new VictorSPX(RobotMap.INTAKE_MOTOR_ID);
		winch = new TalonSRX(RobotMap.INTAKE_WINCH_ID);
		winch.setNeutralMode(NeutralMode.Brake);
		winch.setInverted(false);
	}

	public void setPower(double intakePower) {
		intakeMotor.set(VictorSPXControlMode.PercentOutput, intakePower);
	}

	public void stop() {
		intakeMotor.set(VictorSPXControlMode.PercentOutput, 0);
	}

	public void setIsRetracting(boolean isRetracted) {
		this.isRetracted = isRetracted;
	}

	public boolean getIsRetracted() {
		return isRetracted;
	}


	public void setIsDeployed(boolean isDeployed) {
		this.isDeployed = isDeployed;
	}

	public boolean getIsDeployed() {
		return isDeployed;
	}

	public void stopDeploy() {
		winch.set(TalonSRXControlMode.PercentOutput, 0);
	}

	public void actuateIntake(double pwr) {
		winch.set(TalonSRXControlMode.PercentOutput, pwr);
	}
}
