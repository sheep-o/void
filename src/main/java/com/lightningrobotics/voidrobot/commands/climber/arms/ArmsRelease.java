// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.lightningrobotics.voidrobot.commands.climber.arms;

import com.lightningrobotics.voidrobot.constants.Constants;
import com.lightningrobotics.voidrobot.subsystems.Climber;

import edu.wpi.first.wpilibj2.command.CommandBase;

public class ArmsRelease extends CommandBase {
    Climber climber;
    int climbMode;

    public ArmsRelease(Climber climber, int climbMode) {
        this.climber = climber;
        this.climbMode = climbMode;
        addRequirements(climber);
    }

    @Override
    public void initialize() {
        climber.setArmsTarget(Constants.HOLD_HEIGHT);
    }

    @Override
    public void end(boolean interrupted) {
        //climber.stopArms(); //still not sure if this should be here...
    }

    @Override
    public boolean isFinished() {
        return climber.armsOnTarget();
    }
}