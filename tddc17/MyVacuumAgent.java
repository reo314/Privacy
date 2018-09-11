package tddc17;


import aima.core.environment.liuvacuum.*;
import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.impl.*;

import java.util.Random;

class MyAgentState
{
	public int[][] world = new int[30][30];
	public int initialized = 0;
	final int UNKNOWN 	= 0;
	final int WALL 		= 1;
	final int CLEAR 	= 2;
	final int DIRT		= 3;
	final int HOME		= 4;
	final int ACTION_NONE 			= 0;
	final int ACTION_MOVE_FORWARD 	= 1;
	final int ACTION_TURN_RIGHT 	= 2;
	final int ACTION_TURN_LEFT 		= 3;
	final int ACTION_SUCK	 		= 4;
	
	public int agent_x_position = 1;
	public int agent_y_position = 1;
	public int agent_last_action = ACTION_NONE;
	
	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public int agent_direction = EAST;
	
	MyAgentState()
	{
		for (int i=0; i < world.length; i++)
			for (int j=0; j < world[i].length ; j++)
				world[i][j] = UNKNOWN;
		world[1][1] = HOME;
		agent_last_action = ACTION_NONE;
	}
	// Based on the last action and the received percept updates the x & y agent position
	public void updatePosition(DynamicPercept p)
	{
		Boolean bump = (Boolean)p.getAttribute("bump");

		if (agent_last_action==ACTION_MOVE_FORWARD && !bump)
	    {
			switch (agent_direction) {
                case MyAgentState.NORTH:
                    agent_y_position--;
                    break;
                case MyAgentState.EAST:
                    agent_x_position++;
                    break;
                case MyAgentState.SOUTH:
                    agent_y_position++;
                    break;
                case MyAgentState.WEST:
                    agent_x_position--;
                    break;
			}
	    }
		
	}
	
	public void updateWorld(int x_position, int y_position, int info)
	{
		world[x_position][y_position] = info;
	}

	public int get_curr_info(){
	    return world[agent_x_position][agent_y_position];
    }

    public Boolean is_home(){
	    return world[agent_x_position][agent_y_position] == HOME;
    }

    public void update_direction(Action act){
        switch (agent_direction) {
            case NORTH:
                if(act == LIUVacuumEnvironment.ACTION_TURN_LEFT){
                    agent_direction = WEST;
                } else {
                    agent_direction = EAST;
                }
                break;
            case EAST:
                if(act == LIUVacuumEnvironment.ACTION_TURN_LEFT){
                    agent_direction = NORTH;
                } else {
                    agent_direction = SOUTH;
                }
                break;
            case SOUTH:
                if(act == LIUVacuumEnvironment.ACTION_TURN_LEFT){
                    agent_direction = EAST;
                } else {
                    agent_direction = WEST;
                }
                break;
            case WEST:
                if(act == LIUVacuumEnvironment.ACTION_TURN_LEFT){
                    agent_direction = SOUTH;
                } else {
                    agent_direction = NORTH;
                }
                break;
        }
    }
	
	public void printWorldDebug()
	{
		for (int i=0; i < world.length; i++)
		{
			for (int j=0; j < world[i].length ; j++)
			{
				if (world[j][i]==UNKNOWN)
					System.out.print(" ? ");
				if (world[j][i]==WALL)
					System.out.print(" # ");
				if (world[j][i]==CLEAR)
					System.out.print(" . ");
				if (world[j][i]==DIRT)
					System.out.print(" D ");
				if (world[j][i]==HOME)
					System.out.print(" H ");
			}
			System.out.println("");
		}
	}
}

class MyAgentProgram implements AgentProgram {

	private int initnialRandomActions = 10;
	private Random random_generator = new Random();
	
	// Here you can define your variables!
	public int iterationCounter = 10;
	public MyAgentState state = new MyAgentState();
	private int redundant_clean = 0;
	
	// moves the Agent to a random start position
	// uses percepts to update the Agent position - only the position, other percepts are ignored
	// returns a random action
	private Action moveToRandomStartPosition(DynamicPercept percept) {
		int action = random_generator.nextInt(6);
		initnialRandomActions--;
		state.updatePosition(percept);
		if(action==0) {
		    state.agent_direction = ((state.agent_direction-1) % 4);
		    if (state.agent_direction<0) 
		    	state.agent_direction +=4;
		    state.agent_last_action = state.ACTION_TURN_LEFT;
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (action==1) {
			state.agent_direction = ((state.agent_direction+1) % 4);
		    state.agent_last_action = state.ACTION_TURN_RIGHT;
		    return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		} 
		state.agent_last_action=state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}

	// draw a wall in world map
	private void draw_wall(){
        switch (state.agent_direction) {
            case MyAgentState.NORTH:
                state.updateWorld(state.agent_x_position,state.agent_y_position-1,state.WALL);
                break;
            case MyAgentState.EAST:
                state.updateWorld(state.agent_x_position+1,state.agent_y_position,state.WALL);
                break;
            case MyAgentState.SOUTH:
                state.updateWorld(state.agent_x_position,state.agent_y_position+1,state.WALL);
                break;
            case MyAgentState.WEST:
                state.updateWorld(state.agent_x_position-1,state.agent_y_position,state.WALL);
                break;
        }
    }

    // prerequisite : bump
    private Action get_direction_corner(){
	    int x = state.agent_x_position;
	    int y = state.agent_y_position;
	    Action left = LIUVacuumEnvironment.ACTION_TURN_LEFT;
	    Action right = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
        switch (state.agent_direction) {
            case MyAgentState.NORTH:
                // top right corner, wall on east ?
                if(state.world[x+1][y] == state.WALL){
                    return left;
                }
                // top left corner, wall on west ?
                if(state.world[x-1][y] == state.WALL){
                    return right;
                }
                break;
            case MyAgentState.EAST:
                // top right corner, wall on north ?
                if(state.world[x][y-1] == state.WALL){
                    return right;
                }
                // bottom right corner, wall on south ?
                if(state.world[x][y+1] == state.WALL){
                    return left;
                }
                break;
            case MyAgentState.SOUTH:
                // bottom right corner, wall on east ?
                if(state.world[x+1][y] == state.WALL){
                    return right;
                }
                // bottom left corner, wall on west ?
                if(state.world[x-1][y] == state.WALL){
                    return left;
                }
                break;
            case MyAgentState.WEST:
                // top left corner, wall on north ?
                if(state.world[x][y-1] == state.WALL){
                    return left;
                }
                // bottom left corner, wall on south ?
                if(state.world[x][y+1] == state.WALL){
                    return right;
                }
                break;
        }
        return null;
    }

    // return action needed to get a new unknow cell direction
    private Action act_to_unknow_cell(){
        int x = state.agent_x_position;
        int y = state.agent_y_position;
        Action left = LIUVacuumEnvironment.ACTION_TURN_LEFT;
        Action right = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
        switch (state.agent_direction) {
            case MyAgentState.NORTH:
                // left ?
                if(state.world[x-1][y] == state.UNKNOWN){
                    return left;
                }
                // right ?
                if(state.world[x+1][y] == state.UNKNOWN){
                    return right;
                }
                break;
            case MyAgentState.EAST:
                // top ?
                if(state.world[x][y-1] == state.UNKNOWN){
                    return left;
                }
                // bottom ?
                if(state.world[x][y+1] == state.UNKNOWN){
                    return right;
                }
                break;
            case MyAgentState.SOUTH:
                // right ?
                if(state.world[x+1][y] == state.UNKNOWN){
                    return left;
                }
                // left ?
                if(state.world[x-1][y] == state.UNKNOWN){
                    return right;
                }
                break;
            case MyAgentState.WEST:
                // top ?
                if(state.world[x][y-1] == state.UNKNOWN){
                    return right;
                }
                // bottom ?
                if(state.world[x][y+1] == state.UNKNOWN){
                    return left;
                }
                break;
        }
        return null;
    }

	@Override
	public Action execute(Percept percept) {
		// DO NOT REMOVE this if condition!!!
    	if (initnialRandomActions>0) {
    		return moveToRandomStartPosition((DynamicPercept) percept);
    	} else if (initnialRandomActions==0) {
    		// process percept for the last step of the initial random actions
    		initnialRandomActions--;
    		state.updatePosition((DynamicPercept) percept);
			System.out.println("Processing percepts after the last execution of moveToRandomStartPosition()");
			state.agent_last_action=state.ACTION_SUCK;
	    	return LIUVacuumEnvironment.ACTION_SUCK;
    	}
		
    	// This example agent program will update the internal agent state while only moving forward.
    	// START HERE - code below should be modified!
    	System.out.println("x=" + state.agent_x_position);
    	System.out.println("y=" + state.agent_y_position);
    	System.out.println("dir=" + state.agent_direction);
    	
	    //iterationCounter--;
	    /*if (iterationCounter==0)
	    	return NoOpAction.NO_OP;*/

	    DynamicPercept p = (DynamicPercept) percept;
	    Boolean bump = (Boolean)p.getAttribute("bump");
	    Boolean dirt = (Boolean)p.getAttribute("dirt");
	    Boolean home = (Boolean)p.getAttribute("home");
	    System.out.println("percept: " + p);

        // ------------------------------------
        //  FROM PERCEPT & LAST ACTION
        // ------------------------------------
        int last_cell_info = state.get_curr_info();
	    state.updatePosition((DynamicPercept)percept);
        if(state.is_home()){
            //return NoOpAction.NO_OP;
        }
        int curr_info = state.get_curr_info();


	    if (bump) {
            draw_wall();
	    }
	    if (dirt){
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.DIRT);
        }
	    else{
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.CLEAR);
        }

	    state.printWorldDebug();

	    // ------------------------------------
        //  NEXT ACTION
        // ------------------------------------
        Action act = null;

	    if (dirt) {
	    	System.out.println("DIRT -> choosing SUCK action!");
	    	state.agent_last_action=state.ACTION_SUCK;
            act = LIUVacuumEnvironment.ACTION_SUCK;
	    } else {
            //System.out.println("last cell " + last_cell_info + " curr : "+ curr_info);
            // last cell was already cleared and new one too
            if(curr_info == state.CLEAR && last_cell_info == state.CLEAR){
                if(bump == false){
                    redundant_clean++;
                }
                if(redundant_clean > 2){
                    System.out.println("Hit threshold, try to found new way");
                    // look around and find UNKNOW cell
                    act = act_to_unknow_cell();
                    // act != null mean we can visit an unknow cell
                    if(act != null){
                        redundant_clean = 0;
                    }
                    //System.out.println("Switch !" + act);
                }
            } else {
                redundant_clean = 0;
            }

	    	if (bump) {
                // direction if a corner is reached
                act = get_direction_corner();
                if(act == null){
                    int x = random_generator.nextInt();
                    if(x % 2 == 0){
                        act = LIUVacuumEnvironment.ACTION_TURN_LEFT;
                    } else {
                        act = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
                    }
                }
	    	} else if(act == null) {
	    		state.agent_last_action=state.ACTION_MOVE_FORWARD;
                act = LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	    	}
	    }
        if (act == LIUVacuumEnvironment.ACTION_TURN_LEFT || act == LIUVacuumEnvironment.ACTION_TURN_RIGHT){
            state.agent_last_action = state.ACTION_NONE;
	        state.update_direction(act);
        }
	    return act;
	}
}

public class MyVacuumAgent extends AbstractAgent {
    public MyVacuumAgent() {
    	super(new MyAgentProgram());
	}
}
