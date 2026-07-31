package app.sharehub.security;

import app.sharehub.domain.UserEntity;
import app.sharehub.mapper.UserMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ShareHubUserDetailsService implements UserDetailsService {
    private final UserMapper users;
    public ShareHubUserDetailsService(UserMapper users) { this.users = users; }
    @Override
    public UserDetails loadUserByUsername(String email) {
        UserEntity user = users.selectOne(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getEmail, email));
        if (user == null) throw new UsernameNotFoundException("邮箱或密码错误");
        return ShareHubPrincipal.from(user);
    }
}
