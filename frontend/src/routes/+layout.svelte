<script lang="ts">
  import Modal from '$lib/components/Modal.svelte';
  import { modal } from '$lib/services/modal.service';
  import { onDestroy, onMount } from 'svelte';
  import '../app.css';
  
  let { children } = $props();
  let keepaliveInterval: ReturnType<typeof setInterval>;
  
  async function doKeepalive() {
    console.log('🔄 Keepalive: отправляю запрос...');
    try {
      const response = await fetch('/api/auth/validate', {
        method: 'GET',
        credentials: 'same-origin'
      });
      
      console.log('🔄 Keepalive: ответ', response.status);
      
      if (response.ok) {
        console.log('✅ Keepalive: сессия продлена');
        window.dispatchEvent(new CustomEvent('user-data-updated'));
      } else if (response.status === 401) {
        console.log('❌ Keepalive: сессия истекла');
        window.location.href = '/';
      }
    } catch (e) {
      console.error('❌ Keepalive: ошибка', e);
    }
  }
  
  onMount(() => {
    console.log('📌 Layout: keepalive запущен');
    keepaliveInterval = setInterval(doKeepalive, 780000); // 2 минуты для теста
  });
  
  onDestroy(() => {
    if (keepaliveInterval) clearInterval(keepaliveInterval);
  });
</script>

<svelte:head>
</svelte:head>

<Modal
    isOpen={$modal.isOpen}
    title={$modal.title}
    message={$modal.message}
    type={$modal.type}
    showConfirm={$modal.showConfirm}
    confirmText={$modal.confirmText}
    cancelText={$modal.cancelText}
    onConfirm={() => {
        $modal.onConfirm?.();
        modal.close();
    }}
    onCancel={() => {
        $modal.onCancel?.();
        modal.close();
    }}
    onClose={modal.close}
/>

{@render children()}