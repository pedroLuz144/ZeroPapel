package com.goldenpetiscaria.zeropapel.usuario.repository;

import com.goldenpetiscaria.zeropapel.usuario.entity.Usuario;
import com.goldenpetiscaria.zeropapel.usuario.enumerator.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);

    long countByCargoAndAtivoTrue(Cargo cargo);
}
