yum install python-pip -y

Shadowsocks 客户端

安装

pip install --upgrade pip

pip install shadowsocks

配置

新建配置文件：

vi  /etc/shadowsocks.json

填写以下内容

{
"server":"ec2-3-91-255-201.compute-1.amazonaws.com",

"server_port":9000,

"local_address":"127.0.0.1",

"local_port":1985,

"password":"password0",

"timeout":300,

"method":"aes-256-cfb",

"workers":1

}

启动

nohup sslocal -c /etc/shadowsocks.json  &

echo "nohup sslocal -c /etc/shadowsocks.json  &" /etc/rc.local  #设置自启动

测试

运行 curl --socks5 127.0.0.1:1985 http://httpbin.org/ip，如果返回你的 ss 服务器 ip 则测试成功：

{
"origin":"x.x.x.x"#你的 ss 服务器 ip

}

Privoxy

Shadowsocks 是一个 socket5 服务，我们需要使用 Privoxy 把流量转到 http／https 上。

###下载安装文件

wget http://blog.liuguofeng.com/wp-content/uploads/2018/07/privoxy-3.0.26-stable-src.tar.gz
tar -zxvf privoxy-3.0.26-stable-src.tar.gz
cd privoxy-3.0.26-stable

新建用户

Privoxy 强烈不建议使用 root 用户运行，所以我们使用新建一个用户.

useradd privoxy

安装

yum install install autoconf automake libtool
autoheader  && autoconf
./configure
make  && make install

配置

cat  /usr/local/etc/privoxy/config  |  grep  -Ev  "^$|#"

找到以下两句，确保没有注释掉

listen-address   127.0.0.1:8118                                 #8118是默认端口，不用改，下面会用到
forward-socks5t   /               127.0.0.1:1985 .           # 这里的端口写 shadowsocks 的本地端口（注意最后那个 . 不要漏了）

启动
./privoxy --user privoxy /usr/local/etc/privoxy/config

编辑 /etc/profile

添加下面两句：
export http_proxy=http://127.0.0.1:8118
export https_proxy=http://127.0.0.1:8118

运行以下：

source/etc/profile

测试生效：
curl -I www.google.com


nohup sslocal -c /etc/shadowsocks.json &

./privoxy --user privoxy /usr/local/etc/privoxy/config

如果不需要用代理了，记得把/etc/profile里的配置注释掉，不然会一直走代理流量。