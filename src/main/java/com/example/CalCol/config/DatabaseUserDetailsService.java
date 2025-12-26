package com.example.CalCol.config;

import com.example.CalCol.entity.AppUser;
import com.example.CalCol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser appUser = userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		if (!appUser.getEnabled()) {
			throw new UsernameNotFoundException("User is disabled: " + username);
		}

		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_" + appUser.getRole()));

		return User.builder()
			.username(appUser.getUsername())
			.password(appUser.getPassword())
			.authorities(authorities)
			.disabled(!appUser.getEnabled())
			.build();
	}
}

