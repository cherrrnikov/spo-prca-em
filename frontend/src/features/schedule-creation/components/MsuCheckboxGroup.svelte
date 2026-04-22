<script lang="ts">
    import type { MsuConfig } from '$lib/types';
    
    export let msu: 'msu1' | 'msu2';
    export let config: MsuConfig;
    export let onUpdate: (config: MsuConfig) => void;
    export let disableVd: boolean = false;

    // Все каналы в массиве для удобства
    const allChannels = [
        { type: 'vd', num: 1, key: 'vd1' },
        { type: 'vd', num: 2, key: 'vd2' },
        { type: 'vd', num: 3, key: 'vd3' },
        { type: 'ik', num: 4, key: 'ik4' },
        { type: 'ik', num: 5, key: 'ik5' },
        { type: 'ik', num: 6, key: 'ik6' },
        { type: 'ik', num: 7, key: 'ik7' },
        { type: 'ik', num: 8, key: 'ik8' },
        { type: 'ik', num: 9, key: 'ik9' },
        { type: 'ik', num: 10, key: 'ik10' }
    ];

    $: isAllSelected = allChannels
        .filter(channel => !(channel.type === 'vd' && disableVd))
        .every(channel => config[channel.key as keyof MsuConfig] === 1);

    // Выбрать/снять все каналы
    function toggleAll(checked: boolean) {
        const updatedConfig = { ...config };
        
        allChannels.forEach(channel => {
            if (channel.type === 'vd' && disableVd) return;
            updatedConfig[channel.key as keyof MsuConfig] = checked ? 1 : 0;
        });
        
        updatedConfig.prVdMsu = hasAnyVd(updatedConfig) ? 1 : 0;
        updatedConfig.prIkMsu = hasAnyIk(updatedConfig) ? 1 : 0;
        updatedConfig.prMsu = (hasAnyVd(updatedConfig) || hasAnyIk(updatedConfig)) ? 1 : 0;
        
        onUpdate(updatedConfig);
    }

    function handleVdChange(vdNumber: number, checked: boolean) {
        const updatedConfig = { ...config };
        const vdKey = `vd${vdNumber}` as keyof MsuConfig;
        updatedConfig[vdKey] = checked ? 1 : 0;
        
        updatedConfig.prVdMsu = hasAnyVd(updatedConfig) ? 1 : 0;
        updatedConfig.prMsu = (hasAnyVd(updatedConfig) || hasAnyIk(updatedConfig)) ? 1 : 0;
        
        onUpdate(updatedConfig);
    }

    function handleIkChange(ikNumber: number, checked: boolean) {
        const updatedConfig = { ...config };
        const ikKey = `ik${ikNumber}` as keyof MsuConfig;
        updatedConfig[ikKey] = checked ? 1 : 0;
        
        updatedConfig.prIkMsu = hasAnyIk(updatedConfig) ? 1 : 0;
        updatedConfig.prMsu = (hasAnyVd(updatedConfig) || hasAnyIk(updatedConfig)) ? 1 : 0;
        
        onUpdate(updatedConfig);
    }

    function hasAnyVd(config: MsuConfig): boolean {
        return config.vd1 === 1 || config.vd2 === 1 || config.vd3 === 1;
    }

    function hasAnyIk(config: MsuConfig): boolean {
        return [config.ik4, config.ik5, config.ik6, config.ik7, config.ik8, config.ik9, config.ik10]
            .some(value => value === 1);
    }
</script>

<div class="ts-config-container">
    <div class="ts-header">
        <label class="select-all-label">
            <input 
                type="checkbox"
                checked={isAllSelected}
                on:change={(e) => toggleAll(e.target.checked)}
            />
            <span>Выбрать все</span>
        </label>
    </div>
    <div class="ts-grid">
        {#each allChannels as channel}
            <label class="checkbox-label" class:disabled={channel.type === 'vd' && disableVd}>
                <input 
                    type="checkbox"
                    checked={config[channel.key as keyof MsuConfig] === 1}
                    disabled={channel.type === 'vd' && disableVd}
                    on:change={(e) => {
                        if (channel.type === 'vd') {
                            handleVdChange(channel.num, e.target.checked);
                        } else {
                            handleIkChange(channel.num, e.target.checked);
                        }
                    }}
                />
                <span>{channel.type === 'vd' ? `ВД${channel.num}` : `ИК${channel.num}`}</span>
            </label>
        {/each}
    </div>
</div>

<style>
    .ts-config-container {
        display: flex;
        flex-direction: column;
    }

    .ts-header {
        display: flex;
        align-items: center;
    }

    .select-all-label {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
        font-size: 0.85rem;
        font-weight: 500;
        color: #4299e1;
    }

    .ts-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
    }

    .checkbox-label {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
        font-size: 0.875rem;
    }

    .checkbox-label.disabled {
        opacity: 0.4;
        cursor: not-allowed;
    }
</style>