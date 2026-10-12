-- V268: 바이낸스 주요 입금 네트워크 확장 + 자산별(USDC/USDT/EURC) 페어 5~10개
SET client_min_messages TO WARNING;

-- 네트워크 카탈로그 (기존 TRC20·ERC20 유지, 주요 체인 추가)
INSERT INTO tb_crypto_network (code, display_name, chain_hint, confirmations, pastel_bg, pastel_fg, sort_ord, use_yn) VALUES
 ('TRC20',    'TRON (TRC-20)',           'TRON',     1,  '#FFE4E6', '#BE123C', 1,  'Y'),
 ('ERC20',    'Ethereum (ERC-20)',       'ETH',     12,  '#E0E7FF', '#3730A3', 2,  'Y'),
 ('BEP20',    'BNB Smart Chain (BEP-20)','BSC',      3,  '#FFF3C4', '#854D0E', 3,  'Y'),
 ('POLYGON',  'Polygon',                 'POLYGON',  64, '#EDE9FE', '#6D28D9', 4,  'Y'),
 ('ARBITRUM', 'Arbitrum One',            'ARB',     12,  '#DBEAFE', '#1D4ED8', 5,  'Y'),
 ('OPTIMISM', 'Optimism',                'OP',      12,  '#FEE2E2', '#B91C1C', 6,  'Y'),
 ('AVAXC',    'Avalanche C-Chain',       'AVAX',     1,  '#FFE4E6', '#9F1239', 7,  'Y'),
 ('SOL',      'Solana',                  'SOL',     32,  '#E0E7FF', '#4338CA', 8,  'Y'),
 ('TON',      'TON',                     'TON',      1,  '#CFFAFE', '#0E7490', 9,  'Y'),
 ('BASE',     'Base',                    'BASE',    12,  '#DBEAFE', '#1E40AF', 10, 'Y'),
 ('OPBNB',    'opBNB',                   'OPBNB',    3,  '#FEF9C3', '#A16207', 11, 'Y'),
 ('NEAR',     'NEAR',                    'NEAR',     1,  '#F3F4F6', '#374151', 12, 'Y')
ON CONFLICT (code) DO UPDATE SET
 display_name = EXCLUDED.display_name,
 chain_hint = EXCLUDED.chain_hint,
 confirmations = EXCLUDED.confirmations,
 pastel_bg = EXCLUDED.pastel_bg,
 pastel_fg = EXCLUDED.pastel_fg,
 sort_ord = EXCLUDED.sort_ord,
 use_yn = 'Y';

-- USDT × 10 (바이낸스 주요 입금망)
INSERT INTO tb_crypto_asset_network (asset_code, network_code, contract_hint, decimals, use_yn) VALUES
 ('USDT', 'TRC20',    'TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t', 6, 'Y'),
 ('USDT', 'ERC20',    '0xdac17f958d2ee523a2206206994597c13d831ec7', 6, 'Y'),
 ('USDT', 'BEP20',    '0x55d398326f99059ff775485246999027b3197955', 18, 'Y'),
 ('USDT', 'POLYGON',  '0xc2132d05d31c914a87c6611c10748aeb04b58e8f', 6, 'Y'),
 ('USDT', 'ARBITRUM', '0xfd086bc7cd5c481dcc9c85ebe478a1c0b69fcbb9', 6, 'Y'),
 ('USDT', 'OPTIMISM', '0x94b008aa00579c1307b0ef2c499ad98a8ce58e58', 6, 'Y'),
 ('USDT', 'AVAXC',    '0x9702230a8ea53601f5cd2dc00fdbc13d4df4a8c7', 6, 'Y'),
 ('USDT', 'SOL',      'Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB', 6, 'Y'),
 ('USDT', 'TON',      'EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs', 6, 'Y'),
 ('USDT', 'BASE',     '0xfde4C96c8593536E31F787dA9b7756d71B1B7C', 6, 'Y')
ON CONFLICT (asset_code, network_code) DO UPDATE SET
 contract_hint = EXCLUDED.contract_hint,
 decimals = EXCLUDED.decimals,
 use_yn = 'Y';

-- USDC × 10
INSERT INTO tb_crypto_asset_network (asset_code, network_code, contract_hint, decimals, use_yn) VALUES
 ('USDC', 'ERC20',    '0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48', 6, 'Y'),
 ('USDC', 'BEP20',    '0x8ac76a51cc950d9822d68b83fe1ad97b32cd580d', 18, 'Y'),
 ('USDC', 'POLYGON',  '0x3c499c542cef5e3811e1192ce70d8cc03d5c3359', 6, 'Y'),
 ('USDC', 'ARBITRUM', '0xaf88d065e77c8cc2239327c5edb3a432268e5831', 6, 'Y'),
 ('USDC', 'OPTIMISM', '0x0b2c639c533813f4aa9d7837caf62653d097ff85', 6, 'Y'),
 ('USDC', 'AVAXC',    '0xb97ef9ef8734c71904d8002f8b6bc66dd9c48a6e', 6, 'Y'),
 ('USDC', 'SOL',      'EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v', 6, 'Y'),
 ('USDC', 'BASE',     '0x833589fcd6edb6e08f4c7c32d4f71b54bda02913', 6, 'Y'),
 ('USDC', 'OPBNB',    '', 18, 'Y'),
 ('USDC', 'NEAR',     '', 6, 'Y')
ON CONFLICT (asset_code, network_code) DO UPDATE SET
 contract_hint = COALESCE(NULLIF(EXCLUDED.contract_hint, ''), tb_crypto_asset_network.contract_hint),
 decimals = EXCLUDED.decimals,
 use_yn = 'Y';

-- EURC × 8 (Circle 주요 체인 + 운영 확장)
INSERT INTO tb_crypto_asset_network (asset_code, network_code, contract_hint, decimals, use_yn) VALUES
 ('EURC', 'ERC20',    '0x1abaea1f7c830bd89acc67ec4af516284b1bc33c', 6, 'Y'),
 ('EURC', 'POLYGON',  '', 6, 'Y'),
 ('EURC', 'ARBITRUM', '', 6, 'Y'),
 ('EURC', 'OPTIMISM', '', 6, 'Y'),
 ('EURC', 'BASE',     '', 6, 'Y'),
 ('EURC', 'SOL',      'HzwqbKZw8HxMN6bF2yFZNrht3c2iXXzpKcFu7uBEDKtr', 6, 'Y'),
 ('EURC', 'AVAXC',    '', 6, 'Y'),
 ('EURC', 'BEP20',    '', 18, 'Y')
ON CONFLICT (asset_code, network_code) DO UPDATE SET
 contract_hint = COALESCE(NULLIF(EXCLUDED.contract_hint, ''), tb_crypto_asset_network.contract_hint),
 decimals = EXCLUDED.decimals,
 use_yn = 'Y';

-- 지갑 브랜드: 바이낸스 월렛 등 지원 네트워크 확장
UPDATE tb_crypto_wallet_brand
SET networks_csv = 'TRC20,ERC20,BEP20,POLYGON,ARBITRUM,OPTIMISM,AVAXC,SOL,TON,BASE,OPBNB,NEAR'
WHERE code = 'BINANCE';

UPDATE tb_crypto_wallet_brand
SET networks_csv = 'ERC20,BEP20,POLYGON,ARBITRUM,OPTIMISM,AVAXC,BASE,OPBNB'
WHERE code IN ('METAMASK', 'COINBASE');

UPDATE tb_crypto_wallet_brand
SET networks_csv = 'TRC20,ERC20,BEP20,POLYGON,ARBITRUM,OPTIMISM,AVAXC,SOL,TON,BASE,OPBNB,NEAR'
WHERE code IN ('TRUST', 'OKX', 'TOKENPOCKET', 'BITGET', 'IMTOKEN');

UPDATE tb_crypto_wallet_brand
SET networks_csv = 'SOL'
WHERE code = 'PHANTOM';
