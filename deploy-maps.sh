# either plugin your device and copy the files via USB-detected-device or use the following method
# when starting an ssh server like sshdroid

URL=192.168.0.102
GH=/sdcard/graphhopper/maps/

# if you install sshdroid you can scp your files to your android device

# wget http://mapsforge.googlecode.com/files/berlin.map
# alternatives: http://download.mapsforge.org/maps/
scp -P 2222 berlin.map root@$URL:$GH

# wget http://download.geofabrik.de/osm/europe/germany/berlin.osm.bz2
# bunzip2 berlin.osm.bz2
# ./create-graph.sh /media/SAMSUNG/maps/berlin.osm
scp -r -P 2222 berlin-gh/ root@$URL:$GH

#download osm
#osm file: 
#  http://downloads.cloudmade.com/americas/northern_america/united_states/california#downloads_breadcrumbs
#  http://downloads.cloudmade.com/europe/western_europe/germany/berlin#downloads_breadcrumbs
#
#osm==>graph
#./create-graph.sh ~/osmhome/california.osm
#./create-graph.sh berlin.osm
#mv ~/osmhome/california-gh ~/mnt2/sdcardData/graphhopper/maps/
#
#graphhopper's map file  http://download.mapsforge.org/maps/
#osm==>map file
#http://wiki.openstreetmap.org/wiki/Osmosis
#http://wiki.openstreetmap.org/wiki/Osmosis/Examples
#http://code.google.com/p/mapsforge/wiki/GettingStartedMapWriter
#

