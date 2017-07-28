package ApolloRescue;

import adf.component.AbstractLoader;
import adf.component.tactics.TacticsAmbulanceTeam;
import adf.component.tactics.TacticsFireBrigade;
import adf.component.tactics.TacticsPoliceForce;
import adf.component.tactics.TacticsAmbulanceCentre;
import adf.component.tactics.TacticsFireStation;
import adf.component.tactics.TacticsPoliceOffice;
import ApolloRescue.tactics.ApolloTacticsAmbulanceCentre;
import ApolloRescue.tactics.ApolloTacticsFireStation;
import ApolloRescue.tactics.ApolloTacticsPoliceOffice;
import ApolloRescue.tactics.ApolloTacticsAmbulanceTeam;
import ApolloRescue.tactics.ApolloTacticsFireBrigade;
import ApolloRescue.tactics.ApolloTacticsPoliceForce;

public class ApolloLoader extends AbstractLoader {
    @Override
    public String getTeamName() {
        return "Apollo-Rescue";
    }

    @Override
    public TacticsAmbulanceTeam getTacticsAmbulanceTeam() {
        return new ApolloTacticsAmbulanceTeam();
    }

    @Override
    public TacticsFireBrigade getTacticsFireBrigade() {
        return new ApolloTacticsFireBrigade();
    }

    @Override
    public TacticsPoliceForce getTacticsPoliceForce() {
        return new ApolloTacticsPoliceForce();
    }

    @Override
    public TacticsAmbulanceCentre getTacticsAmbulanceCentre() {
        return new ApolloTacticsAmbulanceCentre();
    }

    @Override
    public TacticsFireStation getTacticsFireStation() {
        return new ApolloTacticsFireStation();
    }

    @Override
    public TacticsPoliceOffice getTacticsPoliceOffice() {
        return new ApolloTacticsPoliceOffice();
    }
}
