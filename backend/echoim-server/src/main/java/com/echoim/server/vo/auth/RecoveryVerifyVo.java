package com.echoim.server.vo.auth;

import java.util.ArrayList;
import java.util.List;

public class RecoveryVerifyVo {

    private String recoveryToken;
    private List<LoginUserVo> accounts = new ArrayList<>();

    public String getRecoveryToken() {
        return recoveryToken;
    }

    public void setRecoveryToken(String recoveryToken) {
        this.recoveryToken = recoveryToken;
    }

    public List<LoginUserVo> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<LoginUserVo> accounts) {
        this.accounts = accounts;
    }
}
