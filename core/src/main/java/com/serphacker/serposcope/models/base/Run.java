/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.models.base;

import com.serphacker.serposcope.models.base.Group.Module;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DurationFormatUtils;


public class Run {
    
    public enum Mode {
        CRON,
        MANUAL
    }
    
    public enum Status {
        RUNNING,
        ABORTING,
        RETRYING,
        DONE_SUCCESS,
        DONE_WITH_ERROR,
        DONE_ABORTED,
        DONE_CRASHED;
    }
    
    int id;
    Mode mode;
    Module module;
    LocalDate day;
    LocalDateTime started;
    LocalDateTime finished;
    Status status;
    int total;
    int done;
    int progress;
    int captchas;
    int errors;
    User user;
    Group group;
    
    public Run(Mode mode, Module module, LocalDateTime started) {
        this.mode = mode;
        this.module = module;
        this.day = started.toLocalDate();
        this.started = started;
        this.status = Status.RUNNING;
    }

    public Run() {
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getDay() {
        return day;
    }

    public LocalDateTime getStarted() {
        return started;
    }

    public LocalDateTime getFinished() {
        return finished;
    }

    public void setFinished(LocalDateTime finished) {
        this.finished = finished;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public void setStarted(LocalDateTime started) {
        this.started = started;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
    
    public int getDone() {
        return done;
    }

    public void setDone(int done) {
        this.done = done;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getCaptchas() {
        return captchas;
    }

    public void setCaptchas(int captchas) {
        this.captchas = captchas;
    }
    
    public void incCaptchas(int captchas) {
        this.captchas += captchas;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }
    
    protected long getRemainingTimeMs(LocalDateTime now){
        if(finished != null || started == null || progress == 0 || progress == 100){
            return 0;
        }
        
        long duration = (Math.abs(Duration.between(started, now).toMillis()) / 1000l)*1000l;
        return (duration / progress) * (100-progress);
    }    
    
    public long getRemainingTimeMs(){
        return getRemainingTimeMs(LocalDateTime.now());
    }
    
    public String getRemainingTimeFormated(){
        return DurationFormatUtils.formatDurationWords(getRemainingTimeMs(), true, true);
    }
    
    public long getDurationMs(){
        if(started == null){
            return 0;
        }
        return Math.abs(Duration.between(started, finished == null ? LocalDateTime.now() : finished).toMillis());
    }
    
    public String getDurationFormated(){
        return DurationFormatUtils.formatDuration(getDurationMs(), "HH:mm:ss");
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public boolean isRunning(){
        return Status.RUNNING.equals(status) || Status.ABORTING.equals(status) || Status.RETRYING.equals(status);
    }
    
}
