import os, csv, sqlite3, shutil

def categorize(dev_type):
    dt = dev_type.lower()
    if any(k in dt for k in ['tv', 'plasma', 'monitor', 'display', 'lcd', 'led', 'oled']):
        return 'TV'
    if any(k in dt for k in ['projector', 'dlp']):
        return 'Projector'
    if any(k in dt for k in ['receiver', 'amp', 'surround', 'tuner', 'soundbar', 'audio', 'lifestyle', 'zone']):
        return 'Audio'
    if any(k in dt for k in ['air condition', 'air_condition', 'ac', 'climate', 'fan', 'heater', 'hvac']):
        return 'AC / Climate'
    if any(k in dt for k in ['satellite', 'cable', 'dss', 'dvr', 'dvb', 'stb', 'apple tv', 'roku', 'fire']):
        return 'Set-Top Box'
    if any(k in dt for k in ['dvd', 'cd', 'disc', 'bluray', 'blu-ray', 'vcr', 'tape', 'cassette', 'mp3', 'media', 'player']):
        return 'Media Player'
    return 'Other'

def main():
    base_src = 'temp_irdb/codes'
    out_db = 'app/src/main/assets/irdb.db'
    if os.path.exists(out_db):
        os.remove(out_db)
    
    conn = sqlite3.connect(out_db)
    cur = conn.cursor()
    
    cur.execute('''
    CREATE TABLE remotes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        brand TEXT NOT NULL,
        device_type TEXT NOT NULL,
        category TEXT NOT NULL,
        model TEXT NOT NULL
    );
    ''')
    
    cur.execute('''
    CREATE TABLE codes (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        remote_id INTEGER NOT NULL,
        function_name TEXT NOT NULL,
        protocol TEXT NOT NULL,
        device INTEGER NOT NULL,
        subdevice INTEGER NOT NULL,
        function INTEGER NOT NULL
    );
    ''')
    
    remote_count = 0
    code_count = 0
    
    brands = sorted(os.listdir(base_src))
    print(f"Processing {len(brands)} brands...")
    
    for brand in brands:
        brand_path = os.path.join(base_src, brand)
        if not os.path.isdir(brand_path):
            continue
        for dev_type in sorted(os.listdir(brand_path)):
            dev_type_path = os.path.join(brand_path, dev_type)
            if not os.path.isdir(dev_type_path):
                continue
            cat = categorize(dev_type)
            for f in os.listdir(dev_type_path):
                if f.endswith('.csv'):
                    model = f[:-4]
                    cur.execute(
                        'INSERT INTO remotes (brand, device_type, category, model) VALUES (?, ?, ?, ?)',
                        (brand, dev_type, cat, model)
                    )
                    remote_id = cur.lastrowid
                    remote_count += 1
                    
                    csv_path = os.path.join(dev_type_path, f)
                    try:
                        with open(csv_path, 'r', encoding='utf-8', errors='ignore') as fp:
                            reader = csv.DictReader(fp)
                            rows = []
                            for row in reader:
                                fn = (row.get('functionname') or '').strip()
                                proto = (row.get('protocol') or '').strip()
                                dev = row.get('device')
                                sub = row.get('subdevice')
                                func = row.get('function')
                                
                                if not fn or not proto:
                                    continue
                                
                                dev_val = int(dev) if dev and dev.strip().lstrip('-').isdigit() else -1
                                sub_val = int(sub) if sub and sub.strip().lstrip('-').isdigit() else -1
                                func_val = int(func) if func and func.strip().lstrip('-').isdigit() else -1
                                
                                rows.append((remote_id, fn, proto, dev_val, sub_val, func_val))
                            
                            if rows:
                                cur.executemany(
                                    'INSERT INTO codes (remote_id, function_name, protocol, device, subdevice, function) VALUES (?, ?, ?, ?, ?, ?)',
                                    rows
                                )
                                code_count += len(rows)
                    except Exception as e:
                        pass
    
    conn.commit()
    print("Building indices...")
    cur.execute('CREATE INDEX idx_remotes_brand ON remotes(brand)')
    cur.execute('CREATE INDEX idx_remotes_cat ON remotes(category)')
    cur.execute('CREATE INDEX idx_remotes_type ON remotes(device_type)')
    cur.execute('CREATE INDEX idx_codes_remote ON codes(remote_id)')
    cur.execute('CREATE INDEX idx_codes_func ON codes(function_name)')
    conn.commit()
    
    cur.execute('VACUUM')
    conn.commit()
    conn.close()
    
    sz = os.path.getsize(out_db)
    print(f"Created {out_db}: {remote_count} remotes, {code_count} codes, {sz / 1024 / 1024:.2f} MB")

if __name__ == '__main__':
    main()
