-- =====================================================================
-- RuLaFHub Alpha Restruktur - Migration 002
-- View untuk Leaderboard Terkumpul (Aggregated)
-- Jalankan SQL ini di Supabase Dashboard -> SQL Editor
-- =====================================================================

-- ---------------------------------------------------------------------
-- VIEW LEADERBOARD TERKUMPUL (AGGREGATED) - Carta Juara RuLaF
--    Mengumpulkan total skor per murid dari semua sesi permainan
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW public.v_leaderboard_terkumpul AS
SELECT
    l.mykid,
    l.nama_murid,
    SUM(l.skor) AS total_skor,
    MAX(l.level_capai) AS level_tertinggi,
    SUM(l.jawapan_betul) AS total_betul,
    SUM(l.jawapan_salah) AS total_salah,
    COUNT(*) AS bilangan_sesi,
    MAX(l.tarikh) AS tarikh_terkini
FROM public.rulaf_leaderboard l
GROUP BY l.mykid, l.nama_murid
ORDER BY total_skor DESC;

-- Grant permissions
GRANT SELECT ON public.v_leaderboard_terkumpul TO anon, authenticated;
