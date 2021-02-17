[root@k8smaster ~]# cat /etc/sysconfig/network-scripts/ifcfg-ens33 
TYPE=Ethernet
BOOTPROTO=static
NM_CONTROLLED=yes
IPADDR=172.21.254.175
NETMASK=255.255.255.0
GATEWAT=192.168.225.2
DNS1=114.114.114.119
DNS2=114.114.114.119
NAME=ens33
UUID=4b3c4458-ddbb-45e2-b348-c998cb402be6
DEVICE=ens33
ONBOOT=yes
[root@k8smaster ~]# /etc/init.d/network restart


双网卡
