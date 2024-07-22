# -*- mode: python ; coding: utf-8 -*-

block_cipher = None

a = Analysis(
    ['cherrypy_server.py'],
    pathex=['.'],  # Add the current directory to the path
    binaries=[],
    datas=[
        # Include your project directory
        ('rabbitmq_project/', 'rabbitmq_project/'),
        ('new_app/', 'new_app/'),
        # Include the logs directory
        ('logs/', 'logs/'),
        # Include the virtual environment site-packages
        ('.venv/Lib/site-packages/', '.venv/Lib/site-packages/'),
    ],
    hiddenimports=[
        'django',
        'pika',
        'dotenv',
        'oracledb',
        'cherrypy',
        'daphne'
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    noarchive=False,
    optimize=0,
)

pyz = PYZ(a.pure, a.zipped_data, cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.datas,
    [],
    name='RUN_LECO_SMS',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=True,
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
)

coll = COLLECT(
    exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=False,
    upx=True,
    upx_exclude=[],
    name='cherrypy_server',
)
