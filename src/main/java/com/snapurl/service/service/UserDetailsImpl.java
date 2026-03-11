package com.snapurl.service.service;

import com.snapurl.service.models.Users;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
@NoArgsConstructor
public class UserDetailsImpl implements UserDetails {


    private static  final  long serialVersionUID = 1L;

    // UserDetailsImpl is a custom implementation of the UserDetails interface from Spring Security.
    // It encapsulates the user's information and authorities (roles) that are used for authentication and authorization purposes in the application.
    // The class includes fields for the user's ID, username, email, password, and a collection of granted authorities.
    // It also provides a static method to build a UserDetailsImpl instance from a Users entity, which is typically retrieved from the database.
    // The getAuthorities(), getPassword(), and getUsername() methods are overridden to return the appropriate values for authentication and authorization processes in Spring Security.

    private Long id;
    private String username;
    private String email;
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(Long id, String username, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(Users user) {
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole());
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(authority)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
