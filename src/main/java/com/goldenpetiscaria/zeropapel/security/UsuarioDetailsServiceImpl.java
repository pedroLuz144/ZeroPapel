package com.goldenpetiscaria.zeropapel.security;

import com.goldenpetiscaria.zeropapel.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String usuario) throws UsernameNotFoundException {
        return usuarioRepository.findByUsuario(usuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + usuario));
    }
}
