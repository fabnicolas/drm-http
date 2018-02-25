package com.fabnicolas.drmhttp.tests;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.fabnicolas.drmhttp.mainpkg.DRMTracker;

public class JUnit_DRMTest{
	public static String result;
	@Test
	public void test(){
		try{
    	// DRM validation system based on web-host. It can be runned from a thread. It will be reused also for further validations/screenshots uploads.
	    DRMTracker dt = new DRMTracker("URL"){
	    	@Override
	    	public void preDRM(){
	    		// It's possible to store serial/UUID from DRM validation system in some public variables for logging purposes, this way.
	    	}
	    	
	    	@Override
	    	public void postDRM(String result){
	    		JUnit_DRMTest.result=result;
	    	}
	    };
	    
	    // A thread will manage EngineTracker Runnable object and execute it in parallel.
	    Thread thread_DRM = new Thread(dt);
	    thread_DRM.run();
	    
	    // In the while, JFrame will be instantiated.
	    System.out.println("Action1");
	    try{
	    	thread_DRM.join();	// If instantiating Controller object finished, let's wait for the end of DRM thread (that will close program in case of failure).
	    	System.out.println("Action2");	// If everything went good, let's show the Controller window.
	    	assertEquals("Server answer equals 'Access OK'?", "Access OK", JUnit_DRMTest.result);
	    }catch(InterruptedException e){
	    	fail("InterruptedException: "+e.getMessage());
	    }
    }catch(IOException e){
    	fail("IOException: "+e.getMessage());
    }
  }

}
