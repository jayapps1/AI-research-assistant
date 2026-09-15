package com.researchassistant.notification;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserDeviceService {
    private final UserDeviceRepository repository;

    public UserDeviceService(UserDeviceRepository repository) {
        this.repository = repository;
    }

    public UserDevice register(User user, DevicePlatform platform, String pushToken, String deviceName) {
        UserDevice device = repository.findByPushToken(pushToken).orElseGet(UserDevice::new);
        device.setUser(user);
        device.setPlatform(platform);
        device.setPushToken(pushToken);
        device.setDeviceName(deviceName);
        device.setActive(true);
        return repository.save(device);
    }

    public List<UserDevice> list(UUID userId) {
        return repository.findAllByUserId(userId);
    }

    public void delete(UUID deviceId, UUID userId) {
        UserDevice device = repository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found."));
        device.setActive(false);
    }
}
