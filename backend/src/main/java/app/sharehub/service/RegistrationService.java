package app.sharehub.service;

import app.sharehub.domain.SystemSettingEntity;
import app.sharehub.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class RegistrationService {
    public static final String KEY = "registration_enabled";
    private final SystemSettingMapper settings;
    public RegistrationService(SystemSettingMapper settings) { this.settings = settings; }
    public boolean enabled() {
        SystemSettingEntity setting = settings.selectById(KEY);
        return setting != null && "true".equals(setting.getSettingValue());
    }
    public void setEnabled(boolean enabled, Long adminId) {
        SystemSettingEntity setting = new SystemSettingEntity();
        setting.setSettingKey(KEY);
        setting.setSettingValue(Boolean.toString(enabled));
        setting.setUpdatedBy(adminId);
        setting.setUpdatedAt(LocalDateTime.now());
        settings.updateById(setting);
    }
}
