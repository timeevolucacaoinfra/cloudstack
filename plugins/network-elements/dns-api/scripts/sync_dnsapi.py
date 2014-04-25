import MySQLdb
import sys
from dnsapi import DNSAPI


def selectNetworks(network_offering_id):
	sql = "SELECT * FROM cloud.networks WHERE network_offering_id=" + network_offering_id + " AND (state='Implemented' OR state='Allocated') AND removed IS NULL;"
	return sql

def findDNSAPIMapping(network_id):
	sql = "SELECT * FROM cloud.dnsapi_network_ref WHERE network_id=" + network_id + ";"
	return sql

def updateDNSAPIMapping(network_id, domain_id, reverse_domain_id):
	sql = "UPDATE cloud.dnsapi_network_ref SET dnsapi_domain_id=" + domain_id + ", dnsapi_reverse_domain_id=" + reverse_domain_id + " WHERE network_id =" + network_id + ";"
	return sql

def listRecordsDB(domain_id):
	sql = "SELECT * FROM cloud.dnsapi_vm_ref WHERE dnsapi_domain_id=" + domain_id + ";"
	return sql

def listReverseRecordsDB(reverse_domain_id):
	sql = "SELECT * FROM cloud.dnsapi_vm_ref WHERE dnsapi_domain_id=" + reverse_domain_id + ";"
	return sql

def selectVMNameDB(vm_id):
	sql = "SELECT name FROM cloud.vm_instance WHERE id=" + vm_id + ";"
	return sql

def updateDNSAPIRecordMapping(vm_id, domain_id, record_id):
	sql = "UPDATE cloud.dnsapi_vm_ref SET dnsapi_record_id=%s WHERE vm_id=%s AND dnsapi_domain_id=%s;" % (record_id, vm_id, domain_id)
	return sql


if (len(sys.argv) != 5):
	print "Usage: python sync_dnsapi.py DBURL DBUSERNAME DBPASSWORD NETW_OFF_ID"
	exit()

# Setup database
dburl = sys.argv[1]
dbusername = sys.argv[2]
dbpassword = sys.argv[3]
print "Connecting to database '%s' with username '%s' and password '%s'" % (dburl, dbusername, dbpassword)
db = MySQLdb.connect(dburl, dbusername, dbpassword, "")
cursor = db.cursor()

dnsapi = DNSAPI()

print "Using network offering %s" % sys.argv[4]
network_offering_id = sys.argv[4]
try:
	cursor.execute(selectNetworks(network_offering_id))
	networks = cursor.fetchall()
except:
	print "Unable to list networks from database"

for network in networks:
 	network_id = network[0]
 	cidr = network[8]
	network_domain = network[23]

	domain_id = dnsapi.get_domain_id_by_name(network_domain)

	octets = cidr.split(".")
	reverse_domain_name = octets[2] + "." + octets[1] + "." + octets[0] + ".in-addr.arpa"
	reverse_domain_id = dnsapi.get_reverse_domain_id_by_name(reverse_domain_name)

	try:
		cursor.execute(findDNSAPIMapping(str(network_id)))
		mapping = cursor.fetchone()
	except:
		print "Unable to get DNS API mapping from DB"

	mapping_domain_id = mapping[2]
	mapping_reverse_domain_id = mapping[3]
	if (not(domain_id == mapping_domain_id and reverse_domain_id == mapping_reverse_domain_id)):
		# Fix mapping
		try:
			cursor.execute(updateDNSAPIMapping(str(network_id), str(domain_id), str(reverse_domain_id)))
			db.commit()
		except:
			print "Unable to update DNS API mapping"
			db.rollback()


	try:
		cursor.execute(listRecordsDB(str(domain_id)))
		records = cursor.fetchall()
	except:
		print "Unable to list records from database"

	try:
		cursor.execute(listReverseRecordsDB(str(reverse_domain_id)))
 		reverse_records = cursor.fetchall()
 	except:
 		print "Unable to list reverse records from database"


 	dnsapi_records = dnsapi.list_records_by_domain(domain_id, 'a')
 	dnsapi_reverse_records = dnsapi.list_records_by_domain(reverse_domain_id, 'ptr')

 	for record in records:
 		try:
 			cursor.execute(selectVMNameDB(str(record[1])))
 			vm_name = cursor.fetchone()[0].lower()
 		except:
 			print "Unable to retrieve virtual machine name for id %s" % record[1]

	 	for dnsapi_record in dnsapi_records:
			if (vm_name != dnsapi_record['a']['name']):
				continue

			if (dnsapi_record['a']['id'] == record[3]):
				# Everything OK.
				continue
			else:
				# Fix mapping
				try:
					cursor.execute(updateDNSAPIRecordMapping(str(record[1]), str(record[2]), str(dnsapi_record['a']['id'])))
					db.commit()
				except:
					db.rollback()
					print "Unable to update record mapping"


 	for reverse_record in reverse_records:
 		try:
 			cursor.execute(selectVMNameDB(str(record[1])))
 			vm_name = cursor.fetchone()[0].lower()
	 		vm_name = vm_name + "." + network_domain
	 	except:
	 		print "Unable to retrieve virtual machine name for id %s" % reverse_record[1]

 		for dnsapi_reverse_record in dnsapi_reverse_records:
 			if (vm_name != dnsapi_reverse_record['ptr']['content']):
 				continue

 			if (dnsapi_reverse_record['ptr']['id'] == reverse_record[3]):
 				# Everything OK.
 				continue
 			else:
 				# Fix mapping
 				try:
 					cursor.execute(updateDNSAPIRecordMapping(str(reverse_record[1]), str(reverse_record[2]), str(dnsapi_reverse_record['ptr']['id'])))
 					db.commit()
 				except:
 					db.rollback()
 					print "Unable to update reverse record mapping"
 				


db.close()
