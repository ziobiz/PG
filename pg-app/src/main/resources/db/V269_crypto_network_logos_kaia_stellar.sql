-- V269: 네트워크 표시명(첨부 UI) · Kaia·Stellar 추가 · 자산 페어
SET client_min_messages TO WARNING;

INSERT INTO tb_crypto_network (code, display_name, chain_hint, confirmations, pastel_bg, pastel_fg, sort_ord, use_yn) VALUES
 ('BASE',     'Base',            'BASE',    12, '#DBEAFE', '#0052FF', 1,  'Y'),
 ('ARBITRUM', 'Arbitrum One',    'ARB',     12, '#E0E7FF', '#213147', 2,  'Y'),
 ('BEP20',    'BNB Smart Chain', 'BSC',      3, '#FEF9C3', '#F0B90B', 3,  'Y'),
 ('ERC20',    'Ethereum',        'ETH',     12, '#F1F5F9', '#627EEA', 4,  'Y'),
 ('OPTIMISM', 'OP Mainnet',      'OP',      12, '#FEE2E2', '#FF0420', 5,  'Y'),
 ('POLYGON',  'Polygon',         'POLYGON', 64, '#F3E8FF', '#8247E5', 6,  'Y'),
 ('KAIA',     'Kaia',            'KAIA',     1, '#F3F4F6', '#111111', 7,  'Y'),
 ('TRC20',    'Tron',            'TRON',     1, '#FFE4E6', '#FF0013', 8,  'Y'),
 ('SOL',      'Solana',          'SOL',     32, '#EDE9FE', '#9945FF', 9,  'Y'),
 ('STELLAR',  'Stellar',         'XLM',      1, '#F3F4F6', '#000000', 10, 'Y'),
 ('AVAXC',    'Avalanche C-Chain','AVAX',    1, '#FFE4E6', '#E84142', 11, 'Y'),
 ('TON',      'TON',             'TON',      1, '#CFFAFE', '#0098EA', 12, 'Y'),
 ('OPBNB',    'opBNB',           'OPBNB',    3, '#FEF9C3', '#F0B90B', 13, 'Y'),
 ('NEAR',     'NEAR',            'NEAR',     1, '#F3F4F6', '#000000', 14, 'Y')
ON CONFLICT (code) DO UPDATE SET
 display_name = EXCLUDED.display_name,
 chain_hint = EXCLUDED.chain_hint,
 pastel_bg = EXCLUDED.pastel_bg,
 pastel_fg = EXCLUDED.pastel_fg,
 sort_ord = EXCLUDED.sort_ord,
 use_yn = 'Y';

-- Kaia · Stellar 페어 (USDT/USDC 각 + EURC Stellar/Kaia)
INSERT INTO tb_crypto_asset_network (asset_code, network_code, contract_hint, decimals, use_yn) VALUES
 ('USDT', 'KAIA',    '', 6, 'Y'),
 ('USDT', 'STELLAR', '', 6, 'Y'),
 ('USDC', 'KAIA',    '', 6, 'Y'),
 ('USDC', 'STELLAR', '', 6, 'Y'),
 ('EURC', 'KAIA',    '', 6, 'Y'),
 ('EURC', 'STELLAR', '', 6, 'Y')
ON CONFLICT (asset_code, network_code) DO UPDATE SET use_yn = 'Y';
