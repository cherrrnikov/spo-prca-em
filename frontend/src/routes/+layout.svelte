<script lang="ts">
  import Modal from '$lib/components/Modal.svelte';
  import { modal } from '$lib/services/modal.service';
  import { onDestroy, onMount } from 'svelte';
  import '../app.css';
  
  let { children } = $props();
  let keepaliveInterval: ReturnType<typeof setInterval>;
  
  function refreshUserDataFromCookie() {
    try {
      const userDataCookie = document.cookie
        .split('; ')
        .find(row => row.startsWith('user_data='));
      
      if (!userDataCookie) return;
      
      // Диспатчим событие чтобы компоненты могли обновиться
      window.dispatchEvent(new CustomEvent('user-data-updated'));
    } catch (e) {
      // тихо
    }
  }
  
  onMount(() => {
    keepaliveInterval = setInterval(async () => {
      try {
        const response = await fetch('/api/auth/validate', {
          method: 'GET',
          credentials: 'same-origin'
        });
        
        if (response.ok) {
          console.log('🔄 Keepalive: сессия активна');
          refreshUserDataFromCookie();
        } else if (response.status === 401) {
          console.log('❌ Keepalive: сессия истекла, редирект');
          window.location.href = '/';
        }
      } catch (e) {
        // тихо падаем
      }
    }, 600000);
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