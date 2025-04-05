package org.example.config;

import lombok.Getter;
import lombok.Setter;


/**
 * @author Tyzzzero
 */
@Getter
@Setter
public class Config {
    private String url;
    private String userName;
    private String password;
    private String town;
    private int retryCount;
    private double globalTimeout;
    private int threadCount;
}