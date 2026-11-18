package com.itson.ciberseguridad.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itson.ciberseguridad.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * PRUEBAS DE SEGURIDAD - BOLA de escritura (hallazgo P1-005, API1:2023).
 *
 * Escenarios (datos sembrados en data.sql: Ana = usuario 1, Beto = usuario 2):
 *   1. Un pedido valido se crea a nombre del usuario autenticado (201).
 *   2. El intento de inyectar "usuario":{"id":2} se rechaza (400) y NO crea
 *      pedidos para Beto.
 *   3. Sin encabezado X-User-Id -> 401.
 *   4. El campo "fecha" del cliente se rechaza (400): la fecha la fija el servidor.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PruebaBolaEscrituraTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Test
    void pedidoValido_seCreaANombreDelUsuarioAutenticado() throws Exception {
        int pedidosDeBetoAntes = pedidoRepository.findByUsuarioId(2L).size();

        mockMvc.perform(post("/api/pedidos")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"Pedido propio de Ana","monto":1200.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("Pedido propio de Ana"))
                .andExpect(jsonPath("$.nombreUsuario").value("Ana"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertEquals(pedidosDeBetoAntes, pedidoRepository.findByUsuarioId(2L).size());
    }

    @Test
    void inyeccionDeUsuarioAjeno_retorna400YNoCreaPedidoParaBeto() throws Exception {
        int pedidosDeBetoAntes = pedidoRepository.findByUsuarioId(2L).size();

        mockMvc.perform(post("/api/pedidos")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"Pedido inyectado a nombre de Beto","monto":99999.00,"fecha":"2026-11-11","usuario":{"id":2}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertEquals(pedidosDeBetoAntes, pedidoRepository.findByUsuarioId(2L).size());
    }

    @Test
    void sinEncabezadoXUserId_retorna401() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"Pedido sin identidad","monto":100.00}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void fechaDelClienteEsRechazada_laFechaLaFijaElServidor() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"Pedido con fecha falsificada","monto":500.00,"fecha":"2020-01-01"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
