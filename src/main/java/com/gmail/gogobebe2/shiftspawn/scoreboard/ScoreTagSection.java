package com.gmail.gogobebe2.shiftspawn.scoreboard;

import com.gmail.gogobebe2.shiftspawn.Participant;
import com.gmail.gogobebe2.shiftspawn.ShiftSpawn;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;

public class ScoreTagSection extends ScoreboardSection {
    public ScoreTagSection(Participant participant, Objective objective, ShiftSpawn plugin) {
        super(participant, objective, plugin);
    }

    @Override
    public void arrangeSection() {
        Player player = getParticipant().getPlayer();
        String name;
        if (player.getName().length() <= 12) {
            name = player.getName();
        } else {
            name = player.getName().substring(0, 12);
        }
        name = name + "_tag";
        setLabel(name, getParticipant().getScore());
    }
}
