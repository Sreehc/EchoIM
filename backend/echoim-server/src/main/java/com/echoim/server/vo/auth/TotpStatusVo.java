package com.echoim.server.vo.auth;

public class TotpStatusVo {

    private boolean enabled;
    private int recoveryCodesRemaining;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRecoveryCodesRemaining() {
        return recoveryCodesRemaining;
    }

    public void setRecoveryCodesRemaining(int recoveryCodesRemaining) {
        this.recoveryCodesRemaining = recoveryCodesRemaining;
    }
}
