package com.marketpulse.user.service;

import com.marketpulse.common.exception.ResourceNotFoundException;
import com.marketpulse.user.dto.UserDto;
import com.marketpulse.user.entity.UserAccount;
import com.marketpulse.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserAccount getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserDto toDto(UserAccount user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public com.marketpulse.user.dto.UserSettingsDto getSettings(Long userId) {
        UserAccount user = getById(userId);
        Double threshold = user.getThresholdPercent() != null ? user.getThresholdPercent() : 3.0;
        return new com.marketpulse.user.dto.UserSettingsDto(threshold);
    }

    @Transactional
    public com.marketpulse.user.dto.UserSettingsDto updateSettings(Long userId, com.marketpulse.user.dto.UserSettingsDto dto) {
        UserAccount user = getById(userId);
        if (dto.thresholdPercent() != null) {
            user.setThresholdPercent(dto.thresholdPercent());
            userRepository.save(user);
        }
        return new com.marketpulse.user.dto.UserSettingsDto(user.getThresholdPercent());
    }
}
