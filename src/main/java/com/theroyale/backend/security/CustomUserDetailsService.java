package com.theroyale.backend.security;

import com.theroyale.backend.errors.UsuarioNoEncontradoException;
import com.theroyale.backend.model.Role;
import com.theroyale.backend.model.UserEntity;
import com.theroyale.backend.repository.RoleRepository;
import com.theroyale.backend.repository.UserEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userEntityRepository.findByUsername(username)
                .orElseThrow(() -> new UsuarioNoEncontradoException(username));

        return new User(user.getUsername(), user.getPassword(), mapRolesToAuthorities(user.getRoles()));
    }

    private Collection<GrantedAuthority> mapRolesToAuthorities(List<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }
}
