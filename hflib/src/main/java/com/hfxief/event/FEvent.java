package com.hfxief.event;

/**
 * trunk
 * com.iss.innoz.event
 *
 * @Author: xie
 * @Time: 2016/10/17 16:29
 * @Description:
 */


public class FEvent extends IEvent{
    public String error;

    public FEvent(String error) {
        this.error = error;
    }
}
