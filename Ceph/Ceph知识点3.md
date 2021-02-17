Ceph搭建监控：https://www.bookstack.cn/read/zxj_ceph/manual_deploy
Openstack对接Ceph：https://robin-2016.github.io/2019/04/08/openstack%E5%AF%B9%E6%8E%A5ceph/
Ceph搭建单点：https://www.bookstack.cn/read/zxj_ceph/manual_deploy

参考博客1：https://www.cnblogs.com/kevingrace/p/9141432.html
参考博客2：https://www.kclouder.cn/centos-7-cephfs/
Ceph博客1：https://amito.me/2018/Ceph-RBD-Mirror-and-Remote-Backup
Ceph博客2：https://ivanzz1001.github.io/records/post/ceph/2018/08/16/ceph-find-file
S3 radosgw-admin操作：https://www.cnblogs.com/kuku0223/p/8257813.html
S3 s3cmd操作：https://blog.frognew.com/2017/02/using-s3-access-ceph-rgw.html
多地容灾情景：

5，专业ceph运维：http://www.strugglesquirrel.com/
6，ceph运维：https://amito.me/
9，ceph运维：https://www.dovefi.com/post/

多功能存储 Ceph S3 管理工具：CloudBerryExplorer

MON监控
OSD节点
RBD块
S3 radosgw
Amazon S3cmd

# 环境：
# 管理节点：CentOS-7  双网卡 50G+200G+100G硬盘  内存、CPU随意
# OSD节点：CentOS-7  双网卡 50G+200G+100G硬盘  内存、CPU随意

节点	    Hostname	IP地址	            属性
Deploy节点	mon1      192.168.225.148       mon1
OSD节点1  	mon2	  192.168.225.172	    mon2
OSD节点1  	mon3	  192.168.225.173	    mon3

########################################################### 安装准备  ###########################################################

# ntp时间同步：
yum install -y ntpdate
yes | cp -f /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
ntpdate us.pool.ntp.org
crontab -l >>/tmp/crontab.bak
echo "10 * * * * /usr/sbin/ntpdate us.pool.ntp.org | logger -t NTP" >> /tmp/crontab.bak
crontab /tmp/crontab.bak
date
# 关闭防火墙
setenforce 0
sed -i -e '/SELINUX=enforcing/d' /etc/selinux/config
sed -i -e '/SELINUXTYPE=targeted/d' /etc/selinux/config
cat >> /etc/selinux/config << EOF
SELINUX=disabled
EOF
systemctl stop firewalld
systemctl disable firewalld

# 每个节点修改
hostnamectl set-hostname mon1
hostnamectl set-hostname mon2
hostnamectl set-hostname mon3
# 所有节点
cat >> /etc/hosts << EOF
192.168.225.148    mon1
192.168.225.172    mon2 
192.168.225.173    mon3
EOF

# 配置yum源
cat > /etc/yum.repos.d/local.repo << EOF
[ceph]
name=x86_64
baseurl=https://mirrors.aliyun.com/ceph/rpm-octopus/el7/x86_64/
gpgcheck=0
[ceph-noarch]
name=noarch
baseurl=https://mirrors.aliyun.com/ceph/rpm-octopus/el7/noarch/
gpgcheck=0
[ceph-arrch64]
name=arrch64
baseurl=https://mirrors.aliyun.com/ceph/rpm-octopus/el7/aarch64/
gpgcheck=0
[ceph-SRPMS]
name=SRPMS
baseurl=https://mirrors.aliyun.com/ceph/rpm-octopus/el7/SRPMS/
gpgcheck=0
EOF

useradd cephadmin 
echo "cephadmin" | passwd --stdin cephadmin 
echo "cephadmin ALL = (root) NOPASSWD:ALL" | sudo tee /etc/sudoers.d/c ephadmin 
sed -i 's/Default requiretty/#Default requiretty/' /etc/sudoers

sudo su - cephadmin 
ssh-keygen 
ssh-copy-id cephadmin@mon1 
ssh-copy-id cephadmin@mon2 
ssh-copy-id cephadmin@mon3

su -
yum install -y ceph-deploy python-pip
yum install -y ceph ceph-radosgw

su - cephadmin 
mkdir my-cluster 
cd my-cluster 
ceph-deploy new mon1 mon2 mon3
cat > ceph.conf<<EOF
public network = 192.168.225.0/24 
cluster network = 192.168.225.0/24
EOF
ceph-deploy mon create-initial
ceph-deploy admin mon1 mon2 mon3
sudo chown -R cephadmin:cephadmin /etc/ceph
ceph -s
for dev in /dev/sdb /dev/sdc
do
ceph-deploy disk zap mon1 $dev 
ceph-deploy osd create mon1 --data $dev 
ceph-deploy disk zap mon2 $dev 
ceph-deploy osd create mon2 --data $dev 
ceph-deploy disk zap mon3 $dev 
ceph-deploy osd create mon3 --data $dev 
done
ceph-deploy mgr create mon1 mon2 mon3
ceph mgr module enable dashboard
ceph -s

yum -y install ceph-radosgw
cd my-cluster
ceph-deploy rgw create mon1 mon2 mon3
vi ceph.conf
[client.rgw.mon1] 
rgw_frontends = "civetweb port=80" 
[client.rgw.mon2] 
rgw_frontends = "civetweb port=80" 
[client.rgw.mon3] 
rgw_frontends = "civetweb port=80"

ceph-deploy --overwrite-conf config push mon1 mon2 mon3
sudo systemctl restart ceph-radosgw@rgw.mon1.service 
sudo systemctl restart ceph-radosgw@rgw.mon2.service 
sudo systemctl restart ceph-radosgw@rgw.mon3.service

########################
vi pool 
.rgw
.rgw.root
.rgw.control
.rgw.gc
.rgw.buckets
.rgw.buckets.index
.rgw.buckets.extra
.log
.intent-log
.usage
.users
.users.email
.users.swift
.users.uid
########################

########################
vi create_pool.sh 
#!/bin/bash

PG_NUM=250
PGP_NUM=250
SIZE=3

for i in `cat pool`
        do
        ceph osd pool create $i $PG_NUM
        ceph osd pool set $i size $SIZE
        done

for i in `cat pool`
        do
        ceph osd pool set $i pgp_num $PGP_NUM
        done

########################

chmod +x create_pool.sh 
./create_pool.sh

# admin节点创建完全权限用户
radosgw-admin user create --uid=zongbu --display-name="zongbu" -- tenant zongbu  --email=zongbu@example.com
radosgw-admin caps add --uid=zongbu --caps="users=full_control"

"access_key": "6THI2D22HRA5FEF9AZ04",
"secret_key": "Y4h77NAFJ2MNBlVtAFM1Gmc9o10BDOzMkW1O9Kmv"

# 
yum install s3cmd -y
s3cmd --configure

cat .s3cfg
access_key = 6THI2D22HRA5FEF9AZ04
secret_key = Y4h77NAFJ2MNBlVtAFM1Gmc9o10BDOzMkW1O9Kmv
host_base = 192.168.225.148
host_bucket = %(bucket).192.168.225.148

s3cmd mb s3://first-bucket