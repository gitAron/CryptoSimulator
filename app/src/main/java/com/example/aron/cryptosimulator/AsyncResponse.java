package com.example.aron.cryptosimulator;

/**
 * Created by Aron on 13.03.2018.
 */

public interface AsyncResponse {
    //interface that has to be implemented by classes so they can get notified
    //when the HTTP process is finished
    void processFinish(String output);
}
