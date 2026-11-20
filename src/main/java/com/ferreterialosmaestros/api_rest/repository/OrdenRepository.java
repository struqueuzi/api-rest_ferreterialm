package com.ferreterialosmaestros.api_rest.repository;

import com.ferreterialosmaestros.api_rest.model.Orden;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Orden o where o.id = :id")
    java.util.Optional<Orden> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);

    List<Orden> findAllByOrderByIdDesc();
    List<Orden> findByClienteCorreoOrderByIdDesc(String correo);
}
