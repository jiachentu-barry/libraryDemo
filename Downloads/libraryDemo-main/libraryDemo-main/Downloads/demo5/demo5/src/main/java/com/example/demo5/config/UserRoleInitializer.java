package com.example.demo5.config;

import java.util.List;

import com.example.demo5.entity.AppUser;
import com.example.demo5.enums.UserRole;
import com.example.demo5.repository.AppUserRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserRoleInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;

    public UserRoleInitializer(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<AppUser> users = appUserRepository.findAll();
        boolean changed = false;

        for (AppUser user : users) {
            if (user.getRole() == null) {
                user.setRole(UserRole.USER);
                changed = true;
            }
        }

        AppUser adminUser = appUserRepository.findById(1L).orElse(null);
        if (adminUser != null && adminUser.getRole() != UserRole.ADMIN) {
            adminUser.setRole(UserRole.ADMIN);
            changed = true;
        }

        if (changed) {
            appUserRepository.saveAll(users);
        }
    }
}