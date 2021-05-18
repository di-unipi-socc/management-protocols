package unipi.di.socc.ramp.core.analyzer;

import java.util.ArrayList;
import java.util.List;

import unipi.di.socc.ramp.core.analyzer.actions.Action;

//kinda wrapping class for clarity
public class Sequence {

    private final List<Action> actions;

    public Sequence() {
        this.actions = new ArrayList<Action>();
    }

    public Sequence(List<Action> actions){
        if(actions == null)
            throw new NullPointerException();
        this.actions = actions;
    }

    public List<Action> getActions() {
        return actions;
    }

    @Override
    public Sequence clone(){
        List<Action> clonedActions = new ArrayList<>();
        for(Action action : this.actions)
            //clonedActions.add(action.clone());
            // TODO cloning actions seems unnecessary, enough to clone lists
            clonedActions.add(action);
        
        return new Sequence(clonedActions);
    }
}
