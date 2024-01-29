package org.kpipe;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class StepWrapper {

    public int execute(String[] args)  {
        state(WrapperState.RUNNING);
        Watchdog.initialize();
        WrapperState result = new CommandExecutor().execute(args);
        state(result);
        done();
        Log.log("CommandExecuter returned state: "+result);
        if (!result.equals(WrapperState.SUCCESS)) {
            return result.ordinal();
        } else {
            return 0;
        }
    }
    private static void done() {
        try (FileOutputStream done = new FileOutputStream(Constants.WORKDIR_DONE)) {
            Log.log("Created file "+Constants.WORKDIR_DONE);
        } catch (FileNotFoundException e) {
            Main.fail("could not create file "+Constants.WORKDIR_DONE, e);
        } catch (IOException e) {
            Main.fail("could not write to file "+Constants.WORKDIR_DONE, e);
        }
    }

    private static void state(WrapperState newState)  {
        Log.log("Setting state file to "+newState);
        try (PrintStream state = new PrintStream(new FileOutputStream(Constants.WORKDIR_STATE))) {
            state.println(newState);
        } catch (FileNotFoundException e) {
            Main.fail("could not create file "+Constants.WORKDIR_STATE, e);
        }
    }

}
