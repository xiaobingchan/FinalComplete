```shell
调用命令：

/opt/anaconda/bin/ansible-playbook /tmp/webserver.yml -i /tmp/inv_local >> /tmp/webserver.log 2>&1

带变量调用：
/opt/anaconda/bin/ansible-playbook /tmp/configure.yml \
              -i /tmp/inv_local \
              --extra-vars "jupyterhub_url=${jupyterhub_url} \
                            backup_bucket_url=${backup_bucket_url} \
                            cert_file=${cert_file} \
                            key_file=${key_file} \
                            metadata_file=${metadata_file}" \
              >> /opt/jupyterhub/playbook.log 2>&1
```