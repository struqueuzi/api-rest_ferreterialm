package com.ferreterialosmaestros.api_rest.repository;

import com.ferreterialosmaestros.api_rest.model.Abono;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AbonoRepository extends JpaRepository<Abono, Long> {
    List<Abono> findByUsuarioIdOrderById(Long usuarioId);
    List<Abono> findAllByOrderById();
}
