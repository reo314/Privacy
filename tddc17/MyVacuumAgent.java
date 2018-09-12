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

    private static final int ROAM = 0;
    private static final int TRY_TAKE_N_DIR = 1;
    private static final int TREND_NW = 2;
    private int inner_state = ROAM;

	// Here you can define your variables!
    private int iterationCounter = 15*15*2;
    // used in TREND_NW if stuck
    private final int iterationUnstuckStep = 5;
    private int iterationUnstuck = 0;

    // back home threshold
    private final int bh_threshold = 150;

    private MyAgentState state = new MyAgentState();
    private final int redundant_clean_step = 3;
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
            case MyAgentState.EAST:
                // top right corner, wall on north ?
                if(state.world[x][y-1] == state.WALL){
                    return right;
                }
                // bottom right corner, wall on south ?
                if(state.world[x][y+1] == state.WALL){
                    return left;
                }
            case MyAgentState.SOUTH:
                // bottom right corner, wall on east ?
                if(state.world[x+1][y] == state.WALL){
                    return right;
                }
                // bottom left corner, wall on west ?
                if(state.world[x-1][y] == state.WALL){
                    return left;
                }
            case MyAgentState.WEST:
                // top left corner, wall on north ?
                if(state.world[x][y-1] == state.WALL){
                    return left;
                }
                // bottom left corner, wall on south ?
                if(state.world[x][y+1] == state.WALL){
                    return right;
                }
        }
        return null;
    }

    // first search of unknown cell
    private Action act_to_unknow_cell_1(){
        int x = state.agent_x_position;
        int y = state.agent_y_position;
        Action act = null;
        Action left = LIUVacuumEnvironment.ACTION_TURN_LEFT;
        Action right = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
        Action forward = LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
        switch (state.agent_direction) {
            case MyAgentState.NORTH:
                // front ?
                if(state.world[x][y-1] == state.UNKNOWN){
                    state.agent_last_action = state.ACTION_MOVE_FORWARD;
                    act = forward;
                }
                // left ?
                if(state.world[x-1][y] == state.UNKNOWN){
                    act = left;
                }
                // right ?
                if(state.world[x+1][y] == state.UNKNOWN){
                    act = right;
                }
                break;
            case MyAgentState.EAST:
                // front ?
                if(state.world[x+1][y] == state.UNKNOWN){
                    state.agent_last_action = state.ACTION_MOVE_FORWARD;
                    act = forward;
                }
                // top ?
                if(state.world[x][y-1] == state.UNKNOWN){
                    act = left;
                }
                // bottom ?
                if(state.world[x][y+1] == state.UNKNOWN){
                    act = right;
                }
                break;
            case MyAgentState.SOUTH:
                // front ?
                if(state.world[x][y+1] == state.UNKNOWN){
                    state.agent_last_action = state.ACTION_MOVE_FORWARD;
                    act = forward;
                }
                // right ?
                if(state.world[x+1][y] == state.UNKNOWN){
                    act = left;
                }
                // left ?
                if(state.world[x-1][y] == state.UNKNOWN){
                    act = right;
                }
                break;
            case MyAgentState.WEST:
                // front ?
                if(state.world[x-1][y] == state.UNKNOWN){
                    state.agent_last_action = state.ACTION_MOVE_FORWARD;
                    act = forward;
                }
                // top ?
                if(state.world[x][y-1] == state.UNKNOWN){
                    act = right;
                }
                // bottom ?
                if(state.world[x][y+1] == state.UNKNOWN){
                    act = left;
                }
                break;
        }
        return act;
    }

    // second search of unknown cell
    private Action act_to_unknow_cell_2(){
        // TODO ?
	int x = state.agent_x_position;
        int y = state.agent_y_position;
        Action act = null;
        Action left = LIUVacuumEnvironment.ACTION_TURN_LEFT;
        Action right = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
        Action forward = LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
        switch (state.agent_direction) {
	            case MyAgentState.NORTH:
	            if((state.world[x][y-1] != state.UNKNOWN) &&
	            	(state.world[x][y+1] != state.UNKNOWN) &&
	            	(state.world[x-1][y] != state.UNKNOWN) &&
	            	(state.world[x+1][y] != state.UNKNOWN)){
	            	if(state.world[x-1][y-1] == UNKNOWN){
	            		if(state.world[x][y-1] != WALL){
	            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
	            			act = forward;
	            		}else if(state.world[x-1][y] != WALL){
	            			state.agent_last_action = state.ACTION_TURN_LEFT;
	            			act = left;
	            		}	
	            	}else if(state.world[x+1][y-1] == UNKNOWN){
	            		if(state.world[x][y-1] != WALL){
	            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
	            			act = forward;
	            		}else if(state.world[x+1][y] != WALL){
	            			state.agent_last_action = state.ACTION_TURN_RIGHT;
	            			act = right;
	            		}
	            	}else{
	            		if(state.world[x][y-2] == state.UNKNOWN && state.world[x][y-1] != state.WALL){
	            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
	            			act = forward;
	            		}else if (state.world[x-2][y] == state.UNKOWN && state.world[x-1][y] != state.WALL){
	            			state.agent_last_action = state.ACTION_TURN_LEFT;
	            			act = left;
	            		}else if (state.world[x+2][y] == state.unknown && state.world[x+1][y] != state.WALL){
	            			state.agent_last_action = state.ACTION_TURN_RIGHT;
	            			act = right;
	            		}
	            	}
               }
               break;
                case MyAgentState.EAST:
                if((state.world[x][y-1] != state.UNKNOWN) &&
                	(state.world[x][y+1] != state.UNKNOWN) &&
                	(state.world[x-1][y] != state.UNKNOWN) &&
                	(state.world[x+1][y] != state.UNKNOWN)){
                	if(state.world[x+1][y-1] == UNKNOWN){
                		if(state.world[x+1][y] != WALL){
                			state.agent_last_action = state.ACTION_MOVE_FORWARD;
                			act = forward;
                		}else if(state.world[x][y-1] != WALL){
                			state.agent_last_action = state.ACTION_TURN_LEFT;
                			act = left;
                		}
                	}else if(state.world[x+1][y+1] == UNKNOWN){
                		if(state.world[x+1][y] != WALL){
                			state.agent_last_action = state.ACTION_MOVE_FORWARD;
                			act = forward;
                		else if(state.world[x][y+1] != WALL){
                			state.agent_last_action = state.ACTION_TURN_RIGHT;
                			act = right;
                		}
                	}else{
                		if(state.world[x][y-2] == state.UNKNOWN && state.world[x][y-1] != state.WALL){
                			state.agent_last_action = state.ACTION_TURN_LEFT;
                			act = left;
                		}else if (state.world[x][y+2] == state.UNKNOWN && state.world[x][y+1] != state.WALL){
                			state.agent_last_action = state.ACTION_TURN_RIGHT;
                			act = right;
                		}else if (state.world[x+2][y] == state.unknown && state.world[x+1][y] != state.WALL){
                			state.agent_last_action = state.ACTION_MOVE_FORWARD;
                			act = forward;
                		}
                	}
               }
               break;
               case MyAgentState.SOUTH:
               if((state.world[x][y-1] != state.UNKNOWN) &&
               	(state.world[x][y+1] != state.UNKNOWN) &&
               	(state.world[x-1][y] != state.UNKNOWN) &&
            	(state.world[x+1][y] != state.UNKNOWN)){
            	if(state.world[x+1][y+1] == UNKNOWN){
            		if(state.world[x][y+1] != WALL){
            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
            			act = forward;
            		}else if(state.world[x+1][y] != WALL){
            			state.agent_last_action = state.ACTION_TURN_LEFT;
            			act = left;
            		}
            	}else if(state.world[x-1][y+1] == UNKNOWN){
            		if(state.world[x][y+1] != WALL){
            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
            			act = forward;
            		}else if(state.world[x-1][y] != WALL){
            			state.agent_last_action = state.ACTION_TURN_RIGHT;
            			act = right;
            		}
            	}else{
            		if(state.world[x][y+2] == state.UNKNOWN && state.world[x][y+1] != state.WALL){
            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
            			act = forward;
            		}else if (state.world[x-2][y] == state.UNKNOWN && state.world[x-1][y] != state.WALL){
            			state.agent_last_action = state.ACTION_TURN_RIGHT;
            			act = right
            		}else if (state.world[x+2][y] == state.unknown && state.world[x+1][y] != state.WALL){
            			state.agent_last_action = state.ACTION_TURN_LEFT;
            			act = left;
            		}
            	}
            }
            break;
            	case MyAgentState.WEST:
            	if((state.world[x][y-1] != state.UNKNOWN) &&
            		(state.world[x][y+1] != state.UNKNOWN) &&
            		(state.world[x-1][y] != state.UNKNOWN) &&
            		(state.world[x+1][y] != state.UNKNOWN)){
            		if(state.world[x-1][y-1] == UNKNOWN){
            			if(state.world[x-1][y] != WALL){
            				state.agent_last_action = state.ACTION_MOVE_FORWARD;
            				act = forward;
            			}else if(state.world[x][y-1] != WALL){
            			state.agent_last_action = state.ACTION_TURN_RIGHT;
            			act = right;
            		}
            	}else if(state.world[x-1][y+1] == UNKNOWN){
            		if(state.world[x-1][y] != WALL){
            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
            			act = forward;
            		}else if(state.world[x][y+1] != WALL){
            			state.agent_last_action = state.ACTION_TURN_LEFT;
            			act = left;
            		}else{
            		if(state.world[x][y-2] == state.UNKNOWN && state.world[x][y-1] != state.WALL){
            			state.agent_last_action = state.ACTION_TURN_RIGHT;
            			act = right;
            		}else if (state.world[x][y+2] == state.UNKNOWN && state.world[x][y+1] != state.WALL){
            			state.agent_last_action = state.ACTION_TURN_LEFT;
            			act = left
            		}else if (state.world[x-2][y] == state.unknown && state.world[x-1][y] != state.WALL){
            			state.agent_last_action = state.ACTION_MOVE_FORWARD;
                    act = forward;
                }
               }
           }
                break;
        }
        return act;
        return null;
    }

    private Action act_to_unknow_cell(){
        Action act = null;
        // act_to_unknow_cell_1
        // act != null means one of ? can be reach
        // ?
        // X ?
        // ?
        act = act_to_unknow_cell_1();
        // second try
        //? . ?
        //  X .
        //? . ?

        // thrid try
        //? ? ? ?
        //. . . ?
        //  X . ?
        //. . . ?
        //? ? ? ?

        // then recursively increase size of search

        // idea: if we are on north priorize cells on south ?
        //       if we are on east --> west etc...
        return act;
    }

    //if already cleaned, cleanFlag=='Y', else, cleanFlag=='N'
    //map size is in width and hight. for example, width==10, height==5
    private static int count;
    private static int MAP;
    private static int width;
    private static int height;
    private static char cleanFlag;
    private Action perceptCleaned() {
        Action act = null;
        for (int j=0; j < state.world[1].length ; j++)
            if(state.world[1][j] == state.WALL)count++;
        if(count==20)MAP=2020;
        if(count==15)MAP=1515;
        if(count==10) {
            for(int j=0; j < state.world[1].length; j++) {
                if(state.world[10][j] != state.WALL) {
                    MAP=510;
                    break;
                }
                MAP=1010;
            }
        }
        if(count==5) {
            for(int j=0; j < state.world[5].length; j++)
                if(state.world[5][j]!=state.WALL) {
                    MAP = 105;
                    break;
                }
            MAP = 55;
        }
        if(MAP==2020) {
            height = 20;
            width = 20;
        }
        if(MAP==1515) {
            height = 15;
            width = 15;
        }
        if(MAP==510) {
            height = 5;
            width = 10;
        }
        if(MAP==1010) {
            height = 10;
            width = 10;
        }
        if(MAP==105) {
            height =10;
            width = 5;
        }
        if(MAP==55) {
            height = 5;
            width = 5;
        }
        int i;
        for(i = 1; i<=height; ++i) {
            for(int j = 1; j<=width; ++j) {
                if(state.world[i][j]==state.DIRT) {
                    cleanFlag='N';
                    break;
                }cleanFlag = 'Y';
            }
        }
        return act;
    }

    private Boolean take_north_direciton_again = false;
    private Action take_north_direction(){
        int x = state.agent_x_position;
        int y = state.agent_y_position;
        Action left = LIUVacuumEnvironment.ACTION_TURN_LEFT;
        Action right = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
        switch (state.agent_direction) {
            case MyAgentState.EAST:
                return left;
            case MyAgentState.SOUTH:
                return right;
            case MyAgentState.WEST:
                take_north_direciton_again = true;
                return right;
        }
        return null;
    }

    // find most closest known cell from home
    private Action trend_nw(){
        Action act = null;
        if(state.agent_direction == MyAgentState.NORTH){
            // go forward on north
            if(state.agent_last_action == state.ACTION_NONE){
                state.agent_last_action=state.ACTION_MOVE_FORWARD;
                act = LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
            } else{
                act = LIUVacuumEnvironment.ACTION_TURN_LEFT;
            }
        }
        else if(state.agent_direction == MyAgentState.WEST){
            // go forward on west
            if(state.agent_last_action == state.ACTION_NONE){
                state.agent_last_action=state.ACTION_MOVE_FORWARD;
                act = LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
            } else{
                act = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
            }
        }
        return act;
    }

    // prerequise : TREND_NW
    //                                              ___
    // detect if we are stuck in a corner like this |
    private Boolean stuck_corner(){
        int x = state.agent_x_position;
        int y = state.agent_y_position;
        return state.world[x-1][y] == state.WALL && state.world[x][y-1] == state.WALL;
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
        System.out.println("cpt=" + iterationCounter);

	    iterationCounter--;
	    if (iterationCounter==0){
	    	return NoOpAction.NO_OP;
        }

	    if (iterationCounter == bh_threshold){
            inner_state = TRY_TAKE_N_DIR;
            System.out.println("State : TRY_TAKE_N_DIR");
        }

	    DynamicPercept p = (DynamicPercept) percept;
	    Boolean bump = (Boolean)p.getAttribute("bump");
	    Boolean dirt = (Boolean)p.getAttribute("dirt");
	    Boolean home = (Boolean)p.getAttribute("home");
	    System.out.println("percept: " + p);

        // ------------------------------------
        //  FROM PERCEPT & LAST ACTION
        // ------------------------------------
        if(home && iterationCounter < bh_threshold){
            return NoOpAction.NO_OP;
        }

        int last_cell_info = state.get_curr_info();
	    state.updatePosition((DynamicPercept)percept);
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
	        if (inner_state == ROAM) {
                // when state was TREND_NW and pacman is stuck in a corner, we roam again while iterationUnstuck > 0
                if(iterationUnstuck-- > 0){
                    if(iterationUnstuck == 0){
                        inner_state = TRY_TAKE_N_DIR;
                    }
                }

	            // roam and get away if we are on redundant cells
                if(curr_info == state.CLEAR && last_cell_info == state.CLEAR){
                    if(bump == false){
                        redundant_clean++;
                    }
                    if(redundant_clean > redundant_clean_step){
                        // look around and find UNKNOW cell, act != null if a we can visit a unknow cell
                        act = act_to_unknow_cell();
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
                        // bump with wall near us, do not take this direction
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
            else if (inner_state == TRY_TAKE_N_DIR){
	            // get north direction before TREND_NW state
                if (state.agent_direction != MyAgentState.NORTH){
                    act = take_north_direction();
                } else {
                    state.agent_last_action=state.ACTION_MOVE_FORWARD;
                    act = LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
                }
                if(state.agent_direction != MyAgentState.SOUTH){
                    inner_state = TREND_NW;
                }
            }
            else if(inner_state == TREND_NW){
                //                                              ___
                // detect if we are stuck in a corner like this |
                if(stuck_corner() == false){
                    act = trend_nw();
                } else{
                    inner_state = ROAM;
                    iterationUnstuck = iterationUnstuckStep;
                    if (state.agent_direction == MyAgentState.NORTH){
                        act = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
                    } else {
                        act = LIUVacuumEnvironment.ACTION_TURN_LEFT;
                    }
                }
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
