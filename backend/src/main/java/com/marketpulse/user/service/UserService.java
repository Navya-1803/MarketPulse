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
}
