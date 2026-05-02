package com.echoim.server.vo.message;

import java.util.List;

public class VoicePayloadVo {

    private Integer duration;
    private List<Double> waveform;

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public List<Double> getWaveform() {
        return waveform;
    }

    public void setWaveform(List<Double> waveform) {
        this.waveform = waveform;
    }
}
