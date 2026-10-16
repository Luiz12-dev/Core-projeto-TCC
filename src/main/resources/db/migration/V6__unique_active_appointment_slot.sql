-- V6: Impede duas reservas ativas com o mesmo horario de inicio
--
-- O AppointmentValidator ja recusa sobreposicao, mas verifica e grava em dois
-- passos: duas requisicoes simultaneas para o mesmo horario passam ambas pela
-- verificacao antes de qualquer uma gravar. Este indice e a garantia no banco
-- para esse caso. Agendamentos cancelados ou concluidos ficam de fora, para que
-- o horario de uma reserva cancelada possa ser reservado de novo.
--
-- Cobre so inicios identicos. Sobreposicao parcial entre servicos de duracoes
-- diferentes continua dependendo apenas do validador.
CREATE UNIQUE INDEX uq_appointments_horario_ativo
    ON appointments (date_time)
    WHERE status NOT IN ('CANCELLED', 'COMPLETED');
