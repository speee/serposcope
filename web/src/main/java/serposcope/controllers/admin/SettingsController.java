/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package serposcope.controllers.admin;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.serphacker.serposcope.db.base.BaseDB;
import static com.serphacker.serposcope.db.base.ConfigDB.APP_PRUNE_RUNS;
import static com.serphacker.serposcope.db.base.ConfigDB.APP_PRUNE_GROUP_RUNS;
import com.serphacker.serposcope.db.base.PruneDB;
import com.serphacker.serposcope.models.base.Config;
import com.serphacker.serposcope.scraper.captcha.solver.AntiCaptchaSolver;
import com.serphacker.serposcope.scraper.captcha.solver.CaptchaSolver;
import com.serphacker.serposcope.scraper.captcha.solver.DeathByCaptchaSolver;
import com.serphacker.serposcope.scraper.captcha.solver.DecaptcherSolver;
import com.serphacker.serposcope.scraper.captcha.solver.ImageTyperzSolver;
import com.serphacker.serposcope.scraper.captcha.solver.TwoCaptchaSolver;

import conf.Maintenance;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import ninja.Context;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.Router;
import ninja.i18n.Messages;
import ninja.params.Param;
import ninja.session.FlashScope;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serposcope.controllers.BaseController;
import serposcope.filters.AdminFilter;
import serposcope.filters.XSRFFilter;
import serposcope.helpers.Validator;
import serposcope.lifecycle.DBSizeUtils;

@FilterWith(AdminFilter.class)
@Singleton
public class SettingsController extends BaseController {
    
    private static final DateTimeFormatter HOUR_MINUTES = DateTimeFormatter.ofPattern("HH:mm");
        
    private static final Logger LOG = LoggerFactory.getLogger(SettingsController.class);
    
    @Inject
    BaseDB baseDB;
    
    @Inject
    Maintenance maintenance;
    
    @Inject
    Router router;
    
    @Inject
    Messages msg;    
    
    @Inject
    PruneDB pruneDB;
    
    @Inject
    DBSizeUtils dbSizeUtils;
    
    public Result settings(){
        
        String diskUsage = dbSizeUtils.getDbUsageFormatted();
        String diskFree = dbSizeUtils.getDiskFreeFormatted();        
        
        return Results
            .ok()
            .render("serverTime", LocalTime.now().format(HOUR_MINUTES))
            .render("config", baseDB.config.getConfig())
            .render("diskUsage", diskUsage)
            .render("diskFree", diskFree)
            .render("runs", baseDB.run.count())
            .render("maintenance", maintenance)
            ;
    }
    
    @FilterWith(XSRFFilter.class)
    public Result update(
        Context context,
        @Param("displayHome") String displayHome,
        @Param("displayGoogleTarget") String displayGoogleTarget,
        @Param("displayGoogleSearch") String displayGoogleSearch,
        @Param("cronTime") String cronTime,
        @Param("maintenance") int maintenanceMode,
        @Param("dbcUser") String dbcUser,
        @Param("dbcPass") String dbcPass,
        @Param("decaptcherUser") String decaptcherUser,
        @Param("decaptcherPass") String decaptcherPass,        
        @Param("anticaptchaApiKey") String anticaptchaApiKey,
        @Param("twoCaptchaKey") String twoCaptchaKey,
        @Param("imageTyperzKey") String imageTyperzKey,
        @Param("pruneRuns") Integer pruneRuns,
        @Param("pruneGroupRuns") Integer pruneGroupRuns
    ){
        FlashScope flash = context.getFlashScope();
        
        Config config = new Config();

        if(cronTime == null || cronTime.isEmpty()){
            config.setCronTime("");
        } else {
            try {
                config.setCronTime(LocalTime.parse(cronTime));
            } catch(Exception ex){
                flash.error("admin.settings.cronTimeError");
                return Results.redirect(router.getReverseRoute(SettingsController.class, "settings"));                
            }
//            Matcher matcher = PATTERN_CRONTIME.matcher(cronTime);
//            if(!matcher.find()){
//                flash.error("admin.settings.cronTimeError");
//                return Results.redirect(router.getReverseRoute(SettingsController.class, "settings"));
//            }
//            config.setCronTime(LocalTime.of(Integer.parseInt(matcher.group(0)), Integer.parseInt(matcher.group(1))));
        }
        
        if(!Validator.isEmpty(dbcUser) && !Validator.isEmpty(dbcPass)){
            config.setDbcUser(dbcUser);
            config.setDbcPass(dbcPass);
        }
        
        if(!Validator.isEmpty(decaptcherUser) && !Validator.isEmpty(decaptcherPass)){
            config.setDecaptcherUser(decaptcherUser);
            config.setDecaptcherPass(decaptcherPass);
        }
        
        if(!Validator.isEmpty(anticaptchaApiKey)){
            config.setAnticaptchaKey(anticaptchaApiKey);
        }
        
        if(!Validator.isEmpty(twoCaptchaKey)){
            config.setTwoCaptchaKey(twoCaptchaKey);
        }
        
        if(!Validator.isEmpty(imageTyperzKey)){
            config.setImageTyperzKey(imageTyperzKey);
        }
        
        if(pruneRuns == null || pruneRuns == 0){
            config.setPruneRuns(0);
        } else {
            config.setPruneRuns(pruneRuns);
        }
        
        if(pruneGroupRuns == null || pruneGroupRuns == 0){
            config.setPruneGroupRuns(0);
        } else {
            config.setPruneGroupRuns(pruneGroupRuns);
        }
        
        if(displayHome != null && !Config.DEFAULT_DISPLAY_HOME.equals(displayHome) && Config.VALID_DISPLAY_HOME.contains(displayHome)){
            config.setDisplayHome(displayHome);
        }
        
        if(displayGoogleTarget != null && !Config.DEFAULT_DISPLAY_GOOGLE_TARGET.equals(displayGoogleTarget) 
            && Config.VALID_DISPLAY_GOOGLE_TARGET.contains(displayGoogleTarget)){
            config.setDisplayGoogleTarget(displayGoogleTarget);
        }
        
        if(displayGoogleSearch != null && !Config.DEFAULT_DISPLAY_GOOGLE_SEARCH.equals(displayGoogleSearch) 
            && Config.VALID_DISPLAY_GOOGLE_SEARCH.contains(displayGoogleSearch)){
            config.setDisplayGoogleSearch(displayGoogleSearch);
        }        
        
        baseDB.config.updateConfig(config);
        maintenance.setEnabled(maintenanceMode > 0);
        
        flash.success("label.settingsUpdated");
        return Results.redirect(router.getReverseRoute(SettingsController.class, "settings"));
    }
    
    @FilterWith(XSRFFilter.class)
    public Result reset(
        Context context
    ){
        baseDB.config.updateConfig(new Config());
        context.getFlashScope().success("label.settingsUpdated");
        return Results.redirect(router.getReverseRoute(SettingsController.class, "settings"));
    }
    
    @FilterWith(XSRFFilter.class)
    public Result prune(
        Context context,
        @Param("pruneRuns") Integer pruneRuns
    ){
        if(pruneRuns == null){
            pruneRuns = 0;
        }
        baseDB.config.updateInt(APP_PRUNE_RUNS, pruneRuns);
        
        if(pruneRuns > 0){
            long pruned = pruneDB.prune(pruneRuns, false);
            LOG.warn("[Prune] {} tasks was pruned before {} days.", pruned, pruneRuns);
            context.getFlashScope().success(msg.get("admin.settings.pruneResult", context, Optional.absent(), pruned).get());
        }
        
        return Results.redirect(router.getReverseRoute(SettingsController.class, "settings"));
    }
    
    @FilterWith(XSRFFilter.class)
    public Result pruneGroup(
        Context context,
        @Param("pruneGroupRuns") Integer pruneGroupRuns
    ){
        if(pruneGroupRuns == null){
        	pruneGroupRuns = 0;
        }
        baseDB.config.updateInt(APP_PRUNE_GROUP_RUNS, pruneGroupRuns);
        
        if(pruneGroupRuns > 0){
            long pruned = pruneDB.prune(pruneGroupRuns, true);
            LOG.warn("[Prune] {} group tasks was pruned before {} days.", pruned, pruneGroupRuns);
            context.getFlashScope().success(msg.get("admin.settings.pruneResult", context, Optional.absent(), pruned).get());
        }
        
        return Results.redirect(router.getReverseRoute(SettingsController.class, "settings"));
    }

    public Result testCaptcha(
        Context context,
        @Param("service") String captchaService,
        @Param("user") String captchaUser,
        @Param("pass") String captchaPass,
        @Param("api") String captchaApiKey
    ){
        
        CaptchaSolver solver = null;
        if(captchaService != null){
            switch(captchaService){
                case "dbc":
                    if(!StringUtils.isEmpty(captchaUser) && !StringUtils.isEmpty(captchaPass)){
                        solver = new DeathByCaptchaSolver(captchaUser, captchaPass);
                    }
                    break;
                case "decaptcher":
                    if(!StringUtils.isEmpty(captchaUser) && !StringUtils.isEmpty(captchaPass)){
                        solver = new DecaptcherSolver(captchaUser, captchaPass);
                    }
                    break;
                case "anticaptcha":
                    if(!StringUtils.isEmpty(captchaApiKey)){
                        solver = new AntiCaptchaSolver(captchaApiKey);
                    }
                    break;
                case "twocaptcha":
                    if(!StringUtils.isEmpty(captchaApiKey)){
                        solver = new TwoCaptchaSolver(captchaApiKey);
                    }
                    break;
                case "imagetyperz":
                    if(!StringUtils.isEmpty(captchaApiKey)){
                        solver = new ImageTyperzSolver(captchaApiKey);
                    }
                    break;                    
            }
        }
        
        if(solver == null){
            return Results.ok().text().render(msg.get("admin.settings.invalidService", context, Optional.absent()).get());
        }
        
        try {
            if(!solver.init()){
                return Results.ok().text().render(msg.get("admin.settings.failedInitService", context, Optional.absent(), solver.getFriendlyName()).get());
            }

            if(!solver.testLogin()){
                return Results.ok().text().render(msg.get("admin.settings.invalidServiceCredentials", context, Optional.absent(), solver.getFriendlyName()).get());
            }

            float balance = solver.getCredit();
            return Results.ok().text().render("OK, balance = " + balance);
        }finally{
            try{solver.close();}catch(Exception ex){}
        }
    }
}
