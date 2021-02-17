Ceph Multisite
0.2672017.02.08 10:31:37字数 3313阅读 2892
说明：多数据中心（multisite）功能oNest v6.1新增的功能，旨在实现异地双活，提供了备份容灾的能力。并且具有多个数据中心供用户选择，存放资源。
主节点在对外提供服务时，用户数据在主节点落盘后即向用户回应“写成功”应答，然后实时记录数据变化的相关日志信息。备节点则实时比较主备数据差异，并及时将差异化数据拉回备节点。异步复制技术适用于远距离的容灾方案，对系统性能影响较小。
介绍
一个realm代表了全局唯一的命名空间，这个命名空间由一个或者多个zonegroup，zonegroup可以包含一个或多个zone，zone包含了桶，桶里包依次存放了对象。
每个realm都有与之对应period（表示一个realm的有效期）。每个period及时地代表了zonegroup的状态和zone的配置。每次需要对zonegroup或者zone做修改的时，需要更新period，并提交。
单个数据中心的配置一般由一个zonegroup组成，这个zonegroup包含一个zone和一个或者多个rgw实例。在这些rgw中可以平衡网关请求。
zone
realm配置是由一个zonegroup和多zone组成，每个zone是由一个或者多个rgw组成。每个zone由自身的ceph集群支撑。在一个zonegroup中，多zone可以提供容灾能力。
zonegroup
在之前的版本称为region，ceph支持多zonegroup，每个zonegroup由一个或多个zone组成。在相同的realm中，在同一个zonegroup中的对象共享一个全局命名空间，在跨zonegroup和zone中具有唯一的对象ID。
realm
从oNest v6.1开始，支持realm的概念。可以是一个zonegroup或者多zonegroup和一个全局唯一的命名空间。多realm提供了支持许多配置和命名空间的能力。
注：
1.元数据在同一realm的zone之间进行同步。
2.实体数据只在同一zonegroup的主zone和从zone之间同步，无法跨zonegroup访问实体数据的信息。
名词解释
缩略语	全称
Realm	域，同一域内的账户在其下属的zone上是通用的。一个域下只能有一个主zonegroup，从的zonegroup可以是0或多个。
Period	表示域的有效期，在域的结构发生变化时，其Period会相应变化。
Zonegroup	Zone的集合，等价于之前的Region。一个Zonegroup下只能有一个主Zone。主Zone和从Zone可以部署在同一集群上，也可以部署在不同的集群上。
Zone	表示独立的一个对象存储区域。
Metadata Sync	用户及桶相关的元数据同步，因元数据更新只能在主zonegroup的主zone上进行，所以同步只能由从的zone发起。
Dada Sync	数据同步。
双活部署
拓扑设计
Realm: oNest
Master Zonegroup: beijing
Master Zone: beiji1
Slave Zone: nanji1
Slave Zonegroup: guangzhou
Master Zone: nanji2
Slave Zone: beiji2
域名配置
IP	域名
192.168.141.129	beiji1.com
192.168.141.130	nanji1.com
192.168.141.131	nanji2.com
192.168.141.132	beiji2.com
注：
1.本文rgw端口默认80，若自定义rgw端口，需要在--url或者--endpoints域名后加上对应的端口。
2.--url和--endpoints也可以用IP地址和端口代替，如：http://192.168.141.129:7480
3.若想在zone和zonegroup配置多个rgw，可以在--endpoints后增加多个rgw，以逗号隔开。如：--endpoints=http://rgw1,http://rgw2
删除pool
搭建realm前最好删除本地的pool，删除前请确认，谨慎操作。若仍需保留这些数据，可以不作删除。
systemctl stop ceph-radosgw.target

for poolname in `rados lspools`;do ceph osd pool delete $poolname $poolname --yes-i-really-really-mean-it;done
创建realm
创建一个全局唯一的命名空间，realm有领域、王国的意思。为了便于管理，规定在这个realm里所有的元数据名称都是全局唯一的，无法创建同名的用户（指uid）和桶（bucket或container）。
执行节点：beiji1.com
radosgw-admin realm create --rgw-realm=oNest --default  
创建Master Zonegroup
zonegroup是数据中心的概念。说到王国的概念，就不能缺少国王，或者我们中国古代的皇帝（master zone），所以这个zonegroup很特别，是zonegroup中的master。
执行节点：beiji1.com
1.删除 Default ZoneGroup并创建Master ZoneGroup
为了前向兼容，所以会存在默认的zonegroup，需要删除。
radosgw-admin zonegroup delete --rgw-zonegroup=default

radosgw-admin zonegroup create --rgw-zonegroup=beijing --endpoints=beiji1.com --master --default
创建Master Zone（Master Zonegroup）
master zonegroup的master zone，或者说是王国中的国王。中央集权，权力较大，所有用户的创建、删除、修改都必须通过他进行，其他zone创建、删除或者修改桶级别的元数据的所有请求都需要转发给他。不通过他创建的用户，则无法创建桶。
执行节点：beiji1.com
1.删除Default Zone并创建Master Zone
为了前向兼容，所以会存在默认的zone，需要删除。
radosgw-admin zone delete --rgw-zone=default

radosgw-admin zone create --rgw-zonegroup=beijing --rgw-zone=beiji1 --endpoints=beiji1.com --access-key=admin --secret=admin --default --master
注：创建zone后生成的新的pool的名称前缀将会包含zone的名称。
2.创建系统用户，并更新提交period
各个zone拉取主主zone的数据中心结构都需要通过这个系统用户，需要获取他的AK和SK。系统用户就像一位总管。
radosgw-admin user create --uid=zone.user --display-name="Zone User" --access-key=admin --secret=admin --system

radosgw-admin period update --commit
3.更新rgw配置，增加如下内容
[client.radosgw.beiji1]
rgw zone=beiji1
rgw zonegroup=beijing
rgw realm=oNest
4.重启用于同步功能的rgw
systemctl restart ceph-radosgw@radosgw.beiji1
创建Slave Zone（Master Zonegroup）
在zonegroup中slave zone主要用于与master zone的数据异地同步备份。当master zone出现故障时，可以将slave zone切成master zone继续对外提供服务。待原master zone故障修复时，可以重新切回，不受数据丢失的影响。具体切换步骤请参照Multisite结构调整章节
执行节点：nanji1.com
1.拉取realm和period
为了在同一realm中，需要拉取master zonegroup的master zone的配置，此配置包含realm的信息，已经创建zonegroup的信息。并将其设置为默认的realm和zonegroup
radosgw-admin realm pull --url=beiji1.com --access-key=admin --secret=admin

radosgw-admin realm default --rgw-realm=oNest
radosgw-admin zonegroup default --rgw-zonegroup=beijing

radosgw-admin period pull --url=beiji1.com --access-key=admin --secret=admin
注：设置默认的realm和zonegroup后在执行radosgw-admin命令时，可以省略--rgw-realm和--rgw-zonegroup的参数
2.删除Default Zone并创建Slave Zone
radosgw-admin zone delete --rgw-zone=default

radosgw-admin zone create --rgw-zonegroup=beijing --rgw-zone=nanji1 --endpoints=nanji1.com --access-key=admin --secret=admin --default
3.更新period
radosgw-admin period update --commit --url=beiji1.com --access-key=admin --secret=admin
4.更新rgw配置，增加如下内容
[client.radosgw.nanji1]
rgw zone=nanji1
rgw zonegroup=beijing
rgw realm=oNest
5.重启用于同步功能的rgw
systemctl start ceph-radosgw@radosgw.nanji1
创建Slave Zonegroup
slave zonegroup也是一个数据中心，只是在slave zonegroup上创建的用户无法实现同步，需要master zonegroup的master zone直接创建，不能进行请求转发，但是各个用户创建、删除bucket可以在slave zonegroup的各个zone上进行，他会将这些请求转发到master zonegroupo的master zone进行日志登记，然后通知各个zone进行同步。
执行节点：nanji2.com
1.拉取realm，设置为默认的realm，并拉取period
radosgw-admin realm pull --url=beiji1.com --access-key=admin --secret=admin

radosgw-admin realm default --rgw-realm=oNest

radosgw-admin period pull --url=beiji1.com --access-key=admin --secret=admin
2.删除 Default ZoneGroup并创建Slave ZoneGroup
radosgw-admin zonegroup delete --rgw-zonegroup=default

radosgw-admin zonegroup create --rgw-zonegroup=guangzhou --endpoints=nanji2.com --default
创建Master Zone（Slave Zonegroup）
执行节点：nanji2.com
1.删除 Default Zone，并创建Master Zone
radosgw-admin zone delete --rgw-zone=default

radosgw-admin zone create --rgw-zonegroup=guangzhou --rgw-zone=nanji2 --endpoints=nanji2.com --access-key=admin --secret=admin --default --master
2.更新period
radosgw-admin period update --commit --url=beiji1.com --access-key=admin --secret=admin
3.更新rgw配置
[client.radosgw.nanji2]
rgw zone=nanji2
rgw zonegroup=guangzhou
rgw realm=oNest
4.重启用于同步功能的rgw
systemctl start ceph-radosgw@radosgw.nanji2
创建Slave Zone（Slave Zonegroup）
执行节点：beiji2.com
1.拉取realm和period，设置为默认的realm和zonegroup
radosgw-admin realm pull --url=beiji1.com --access-key=admin --secret=admin

radosgw-admin realm default --rgw-realm=oNest
radosgw-admin zonegroup default --rgw-zonegroup=guangzhou

radosgw-admin period pull --url=beiji1.com --access-key=admin --secret=admin
2.删除Default ZoneGroup和Default Zone，创建Slave Zone
radosgw-admin zonegroup delete --rgw-zonegroup=default
radosgw-admin zone delete --rgw-zone=default

radosgw-admin zone create --rgw-zonegroup=guangzhou --rgw-zone=beiji2 --endpoints=beiji2.com --access-key=admin --secret=admin --default
3.更新period
radosgw-admin period update --commit --url=beiji1.com --access-key=admin --secret=admin
4.更新rgw配置
[client.radosgw.beiji2]
rgw zone=beiji2
rgw zonegroup=guangzhou
rgw realm=oNest
5.重启用于同步功能的rgw
systemctl start ceph-radosgw@radosgw.beiji2
Multisite命令使用
查看multisite结构命令
同步状态查看
分别在zone所在的节点上执行以下命令查看状态
radosgw-admin sync status
1.在主zone一般会得到如下的同步状态包含：
metadata sync
data sync
[root@node3 ~]# radosgw-admin sync status
          realm 2ad3481a-634e-4273-ab95-761debd0cad1 (oNest)
      zonegroup 2bedeaa6-f9b5-40cf-a268-db57ef6d814f (guangzhou)
           zone b290102d-08fd-4291-b196-bb1e6e9412fd (nanji2)
  metadata sync no sync (zone is master)
      data sync source: e63b03b7-b565-465f-a189-9ac6f6218870 (beiji2)
                        syncing
                        full sync: 0/128 shards
                        incremental sync: 128/128 shards
                        data is caught up with source
注：
metadata sync no sync表示该节点是主zone
data is caught up with source表示数据已同步
2.在从zone一般会得到如下的同步状态包含：
metadata sync
data sync
[root@node4 ~]# radosgw-admin sync status
          realm 2ad3481a-634e-4273-ab95-761debd0cad1 (oNest)
      zonegroup 2bedeaa6-f9b5-40cf-a268-db57ef6d814f (guangzhou)
           zone e63b03b7-b565-465f-a189-9ac6f6218870 (beiji2)
  metadata sync syncing
                full sync: 0/64 shards
                metadata is caught up with master
                incremental sync: 64/64 shards
      data sync source: b290102d-08fd-4291-b196-bb1e6e9412fd (nanji2)
                        syncing
                        full sync: 0/128 shards
                        incremental sync: 128/128 shards
                        data is caught up with source
注：
metadata is caught up with master表示元数据已同步
data is caught up with source表示数据已同步
查看realm
列出和获取节点的realm
radosgw-admin realm list

radosgw-admin realm get --rgw-realm=<realm_name> [> filename.json]
注：获取默认的realm，可省略--rgw-realm，若想要获取到本地可加入方括号内容。
列出realm的periods
radosgw-admin realm list-periods
查看zonegroup
列出和获取节点的zonegroup
radosgw-admin zonegroup list

radosgw-admin zonegroup get --rgw-zonegroup=<zonegroup_name> [> filename.json]
注：获取默认的zonegroup，可省略--rgw-zonegroup，若想要获取到本地可加入方括号内容。
获取zonegroup map
radosgw-admin zonegroup-map get
注：此操作可以获取realm下的所有zonegroup结构信息。
查看zone
列出和获取节点的zone
radosgw-admin zone list

radosgw-admin zone get --rgw-zone=<zone_name> [> filename.json]
注：获取默认的zone，可省略--rgw-zone，若想要获取到本地可加入方括号内容。
修改multisite结构命令
Realm
1.指定realm为默认
radosgw-admin realm default --rgw-realm=<realm_name>
2.删除realm
radosgw-admin realm delete --rgw-realm=<realm_name>
3.修改realm信息
radosgw-admin realm set --rgw-realm=<realm_name> --infile=<infilename>
注：可以结合radosgw-admin realm get进行修改
4.拉取realm
radosgw-admin realm pull --url={url-to-master-zone-gateway} --access-key={access-key} --secret={secret}
5.realm重命名
radosgw-admin realm rename --rgw-realm=<current-name> --realm-new-name=<new-realm-name>
Zonegroup
1.指定Zonegroup为默认
radosgw-admin zonegroup default --rgw-zonegroup=<zonegroup_name>
radosgw-admin period update --commit
2.把Zone加入到Zonegroup中
radosgw-admin zonegroup add --rgw-zonegroup=<zonegroup_name> --rgw-zone=<zone_name>
radosgw-admin period update --commit
3.Zonegroup中移除zone
radosgw-admin zonegroup remove --rgw-zonegroup=<zonegroup_name> --rgw-zone=<zone_name>
radosgw-admin period update --commit
4.Zonegroup重命名
radosgw-admin zonegroup rename --rgw-zonegroup=<zonegroup_name> --zonegroup-new-name=<zonegroup_name>
5.删除Zonegroup
radosgw-admin zonegroup delete --rgw-zonegroup=<zonegroup_name>
radosgw-admin period update --commit
6.修改zonegroup信息
radosgw-admin zonegroup set --infile zonegroup.json
radosgw-admin period update --commit
注：可以结合radosgw-admin zonegroup get进行修改
7.修改zonegroup map信息
radosgw-admin zonegroup-map set --infile zonegroupmap.json
radosgw-admin period update --commit
注：可以结合radosgw-admin zonegroup-map get进行修改
Zone
1.删除Zone
radosgw-admin zonegroup remove --zonegroup=<zonegroup_name> --zone=<zone_name>
radosgw-admin period update --commit
radosgw-admin zone delete --rgw-zone<zone_name>
radosgw-admin period update --commit
注：首先从zonegroup中移除该zone，然后进行本地删除。如果直接删zone，更新period会出错。
2.修改Zone
通过命令行修改
radosgw-admin zone modify [options]
radosgw-admin period update --commit
注：
1.这里选项[options]可以是--access-key=<key>，--secret/--secret-key=<key>，--master，--default，--endpoints=<list>，--read-only
2.修改为只读后该zone无法写数据。
通过配置文件修改
radosgw-admin zone set --rgw-zone=<zone_name> --infile zone.json
radosgw-admin period update --commit
注：可以结合radosgw-admin zonegroup-map get进行修改
3.Zone重命名
radosgw-admin zone rename --rgw-zone=<name> --zone-new-name=<name>
radosgw-admin period update --commit
Multisite结构调整
不同Realm下无从zone的Zonegroup合并
说明：将在不同realm下的两个zonegroup组合成同一个realm，并且zonegroup中只含有master zone，不存在slave zone。
注：不建议合并前在两个master zone上写入数据，会有同名元数据被覆盖的风险，若能规避同名的元数据，则可以如下进行尝试。
拓扑结构
原拓扑结构
数据中心1（site1）
Realm: oNest-1
Master Zonegroup: master_zonegroup
Master Zone: master_master
数据中心2（site2）
Realm: oNest-1
Master Zonegroup: slave_zonegroup
Master Zone: slave_master
目标拓扑结构
Realm: oNest-1
Master Zonegroup: master_zonegroup
Master Zone: master_master
Slave Zonegroup: slave_zonegroup
Master Zone: slave_master
部署过程
1.site2拉取site1的realm和period，并将拉取的realm设置为默认
radosgw-admin realm pull --url=http://192.168.141.142:7480 --access-key=admin --secret=admin
radosgw-admin period pull --url=http://192.168.141.142:7480 --access-key=admin --secret=admin

radosgw-admin realm default --rgw-realm=oNest-1
2.修改site2上slave_zonegroup的realm，并把默认的主zonegroup改为从zonegroup
radosgw-admin zonegroup modify --rgw-zonegroup=slave_zonegroup --rgw-realm=oNest-1 --master=false
3.修改site2上zone的realm和默认的zonegroup和zone
radosgw-admin zone modify --rgw-zone=slave_master --rgw-realm=oNest-1

radosgw-admin zonegroup default --rgw-zonegroup=slave_zonegroup
radosgw-admin zone default --rgw-zone=slave_master
4.删除原来的realm，并更新period
radosgw-admin realm delete --rgw-realm=oNest-2

radosgw-admin period update --commit --url=http://192.168.141.142:7480 --access-key=admin --secret=admin
5.重启用来做同步的rgw
注意：提交period之后，可能会在主zonegroup中带入从主zone，若遇到这种情况可以执行以下命令：
radosgw-admin zonegroup remove --rgw-zonegroup=master_zonegroup --rgw-zone=slave_master
同一Zonegroup中主从Zone切换
说明：如果一个zonegroup中主zone挂了，此时需要涉及到把从zone设置为主zone，待原主zone恢复后，需要切换回来。
将原从zone切换为主zone：
1.将从zone设置为默认的主zone，此时查看zonegroup的信息会发现原主zone会自动变为从zone。并更新period
radosgw-admin zone modify --rgw-zone=<zone_name> --master --default

radosgw-admin period update --commit
注：如果从zone原来是只读的，这里切换需要在上述命令后增加--read-only=False
2.重启原从zone上用于同步的rgw。
原主zone故障恢复后，将原主zone设置为主zone。
1.在原主zone上拉取当前主zone的period信息，并将原主zone设置为默认的主zone，更新period
radosgw-admin period pull --url=<url-to-master-zone-gateway> --access-key=<access-key> --secret=<secret>

radosgw-admin zone modify --rgw-zone=<zone_name> --master --default

radosgw-admin period update --commit
2.重启原主上用户同步的rgw
3.如果需要将从zone设置为只读，可以执行以下操作，否则请跳过此步。
radosgw-admin zone modify --rgw-zone=<zone_name> --read-only
radosgw-admin period update --commit
4.重启用于同步的rgw
默认的单数据中心迁移到多数据中心
使用以下的步骤可以将使用默认的zonegroup和zone的数据中心迁移到多数据中心
1.创建realm
radosgw-admin realm create --rgw-realm=<name> --default
2.将默认的zonegroup和zone的名称设置成指定的名称
radosgw-admin zonegroup rename --rgw-zonegroup=default --zonegroup-new-name=<zonegroup_name>

radosgw-admin zone rename --rgw-zone=default --zone-new-name=<zone_name> --rgw-zonegroup=<zonegroup_name>
3.配置主zonegroup，设置其关联的realm，endpoints等
radosgw-admin zonegroup modify --rgw-realm=<realm_name> --rgw-zonegroup=<zonegroup_name> --endpoints=http://<fqdn>:80 --master --default
4.配置主zone，设置其关联的realm，zonegroup和endpoints等
radosgw-admin zone modify --rgw-realm=<name> --rgw-zonegroup=<name> --rgw-zone=<name> --endpoints http://<fqdn>:80 --access-key=<access-key> --secret=<secret-key> --master --default
5.创建系统用户，更新并提交period
radosgw-admin user create --uid=<user-id> --display-name="<display-name>" --access-key=<access-key> --secret=<secret-key> --system

radosgw-admin period update --commit
6.重启用户同步的rgw。
存储策略Placement设置
功能介绍
ceph对象网关通过placement targets进行存储桶和对象的数据，通过placement target来指定桶和对象的存储池。如果不配置placement targets，桶和对象将会存到网关实例所在zone配置的默认存储池中。
存储策略给对象网关提供了一个获得存储策略的权限，比如，指定特殊的存储类型（SSDs，SAS drivers， SATA drivers）
说明：本节主要涉及到zone和zonegroup关于placement的配置，以下操作以配置纠删码（EC）存储池为例，说明多数据中心的placement配置规则和使用方法。
配置过程
1.暂停zone所有的rgw
radosgw-admin stop ceph-radosgw.target
2.修改cursh map
ceph osd getcrushmap -o my-crush-map;crushtool -d my-crush-map -o my-crush-map.txt;vim my-crush-map.txt
增加以下内容：（以单节点ceph集群为例）
rule ec_ruleset {
        ruleset 1
        type erasure
        min_size 2
        max_size 4
        step take default
        step chooseleaf indep 0 type osd
        step emit
}
设置修改后的crush map
crushtool -c my-crush-map.txt -o my-crush-map-new;ceph osd setcrushmap -i my-crush-map-new
3.修改纠删码的profile
ceph osd erasure-code-profile set ec-profile plugin=isa k=2 m=1 ruleset-root=default
4.创建pool并设置其使用的纠删码规则，并让rgw识别新建的pool
ceph osd pool create master_master.rgw.buckets.data.ec 8 8 erasure ec-profile erasecode_ruleset

radosgw-admin pool add --pool master_master.rgw.buckets.data.ec
5.主、从zone中在placement_pools下添加新的ec-placement
radosgw-admin zone get > zone.json [--rgw-zone=<zone_name>]
vim zone.json
"placement_pools": [
        {
            "key": "default-placement",
            "val": {
                "index_pool": "master_master.rgw.buckets.index",
                "data_pool": "master_master.rgw.buckets.data",
                "data_extra_pool": "master_master.rgw.buckets.non-ec",
                "index_type": 0
            }
        },
        {
            "key": "ec-placement",
            "val": {
                "index_pool": "master_master.rgw.buckets.index",
                "data_pool": "master_master.rgw.buckets.data.ec",
                "data_extra_pool": "master_master.rgw.buckets.non-ec",
                "index_type": 0
            }
        }
    ]
radosgw-admin zone set < zone.json [--rgw-zone=<zone_name>]
radosgw-admin period update --commit
注：主、从zone都需要增加对应的ec-placement否则，如果只设置一方的zone，会不起作用。若操作的不是默认的zone，需要加上--rgw-zone=<zone_name>
6.zonegroup中在placement_pools下添加新的ec-placement
radosgw-admin zonegroup get > zonegroup.json
vim zonegroup.json
"placement_targets": [
        {
            "name": "default-placement",
            "tags": []
        },
        {
            "name": "ec-placement",
            "tags": []
        }
    ]
radosgw-admin zonegroup set < zonegroup.json
radosgw-admin period update --commit
注：
1.当拉取realm时，若碰到permission denied的告警，请查看集群之间的时钟差异，若出现在半小时以上，会出现此警告信息。
2.在使用s3cmd创建bucket时，可以指定zonegroup和placement，但有以下区别
--bucket-location=<zonegroup_name>是指定zonegroup的名称
--bucket-location=:<placement_name>是指定placement的名称（多了冒号）
