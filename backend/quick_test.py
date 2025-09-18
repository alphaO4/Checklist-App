import requests

# Login
login_resp = requests.post('http://10.20.1.108:8000/auth/login', json={'username': 'admin', 'password': 'admin'})
token = login_resp.json()['access_token']
headers = {'Authorization': f'Bearer {token}'}

# Test the endpoint that was failing
resp = requests.get('http://10.20.1.108:8000/checklists/7/vehicles', headers=headers)
print(f'Status: {resp.status_code}')

if resp.status_code == 200:
    data = resp.json()
    print('✅ Success! Endpoint fixed.')
    print(f'Checklist: {data["checklist_name"]}')
    print(f'Available vehicles: {len(data["available_vehicles"])}')
    for vehicle in data.get('available_vehicles', []):
        fahrzeugtyp = vehicle.get('fahrzeugtyp', {})
        type_name = fahrzeugtyp.get('name', 'N/A') if fahrzeugtyp else 'N/A'
        print(f'  - {vehicle["kennzeichen"]} (Type: {type_name})')
else:
    print('❌ Still failing:', resp.text)

# Also create a checklist for vehicle group 2 to help with testing
checklist_data = {
    'name': 'Daily Inspection - Group 2',
    'fahrzeuggruppe_id': 2,
    'template': False,
    'items': [
        {'beschreibung': 'Engine Oil Level', 'item_type': 'standard', 'pflicht': True},
        {'beschreibung': 'Battery Status', 'item_type': 'standard', 'pflicht': True}
    ]
}

resp2 = requests.post('http://10.20.1.108:8000/checklists', json=checklist_data, headers=headers)
if resp2.status_code == 201:
    print(f'\n✅ Created checklist for group 2: {resp2.json()["name"]} (ID: {resp2.json()["id"]})')
else:
    print(f'\n⚠️ Checklist creation result: {resp2.status_code}')